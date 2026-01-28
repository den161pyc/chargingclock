package com.example.chargingclock

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class WeatherManager(
    private val context: Context,
    private val repository: WeatherRepository,
    private val weatherLayout: LinearLayout,
    private val tvLocation: TextView,
    private val tvTemp: TextView,
    private val tvCondition: TextView,
    private val tvMiniWeather: TextView,
    private val composeView: ComposeView,
    private val composeColorState: MutableState<ComposeColor>,
    private val composeConditionState: MutableState<String>,
    private val handler: Handler
) {
    private var lat = "55.75"
    private var lon = "37.62"
    var isAutoLocationEnabled = false

    private var composeIsAnimatingState = mutableStateOf(true)
    private val animDuration = 15000L
    private val animPause = 5 * 60 * 1000L
    private val animRunnable = Runnable { startAnimationCycle() }

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isAutoLocationEnabled) requestLocation() else fetchWeather()
            handler.postDelayed(this, 3600000)
        }
    }

    // ... методы initCompose, startUpdates, stopUpdates, startAnimationCycle, setDefaults, getCityNameLocally
    // остаются БЕЗ ИЗМЕНЕНИЙ (как в предыдущем ответе) ...
    // Я приведу только измененный метод fetchWeather

    fun initCompose() { /* код тот же */
        composeView.setContent {
            val color = composeColorState.value
            val condition = composeConditionState.value
            val isAnimating = composeIsAnimatingState.value
            val bg = ComposeColor.Black

            when (condition) {
                "clear" -> AnimatedClearIcon(Modifier.fillMaxSize(), color, isAnimating = isAnimating)
                "partly-cloudy" -> AnimatedPartlyCloudyIcon(Modifier.fillMaxSize(), color, bg, isAnimating = isAnimating)
                "cloudy", "overcast" -> AnimatedOvercastIcon(Modifier.fillMaxSize(), color, bg, isAnimating = isAnimating)
                "drizzle", "light-rain", "rain", "moderate-rain", "heavy-rain", "showers", "continuous-heavy-rain" -> AnimatedRainIcon(Modifier.fillMaxSize(), color, bg, isAnimating = isAnimating)
                "wet-snow", "light-snow", "snow", "hail", "snow-showers" -> AnimatedSnowIcon(Modifier.fillMaxSize(), color, bg, isAnimating = isAnimating)
                "thunderstorm", "thunderstorm-with-rain", "thunderstorm-with-hail" -> AnimatedStormIcon(Modifier.fillMaxSize(), color, bg, isAnimating = isAnimating)
                else -> AnimatedClearIcon(Modifier.fillMaxSize(), color, isAnimating = isAnimating)
            }
        }
    }

    fun startUpdates() {
        handler.removeCallbacks(updateRunnable)
        updateRunnable.run()
        handler.removeCallbacks(animRunnable)
        startAnimationCycle()
    }

    fun stopUpdates() {
        handler.removeCallbacks(updateRunnable)
        handler.removeCallbacks(animRunnable)
    }

    private fun startAnimationCycle() {
        composeIsAnimatingState.value = true
        handler.postDelayed({
            composeIsAnimatingState.value = false
            handler.postDelayed(animRunnable, animPause)
        }, animDuration)
    }

    // --- ОБНОВЛЕННЫЙ МЕТОД ЗАГРУЗКИ ---
    private fun fetchWeather() {
        val prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

        // 0 = Yandex (default), 1 = OpenWeatherMap
        val providerId = prefs.getInt("WEATHER_PROVIDER_ID", 0)

        // Выбираем ключ в зависимости от провайдера
        val apiKey = if (providerId == 0) {
            prefs.getString("YANDEX_API_KEY", "")
        } else {
            prefs.getString("OWM_API_KEY", "")
        }

        if (apiKey.isNullOrEmpty()) {
            setDefaults()
            tvCondition.text = if (providerId == 0) "Нужен Yandex Key" else "Нужен OWM Key"
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            // Передаем providerId в репозиторий
            val data = repository.fetchWeather(lat, lon, apiKey, providerId)

            if (data != null) {
                tvTemp.text = "${if(data.temp > 0) "+" else ""}${data.temp}°"
                tvCondition.text = data.conditionRu
                tvLocation.text = "${data.cityName} ➤"

                composeConditionState.value = data.condition
                composeView.visibility = View.VISIBLE

                tvMiniWeather.text = "${data.iconEmoji} ${if(data.temp > 0) "+" else ""}${data.temp}° ${data.conditionRu}"
            } else {
                setDefaults()
                tvCondition.text = "Ошибка загрузки"
            }
        }
    }

    private fun setDefaults() {
        composeConditionState.value = "clear"
        composeView.visibility = View.VISIBLE
        tvTemp.text = "0°"
        tvMiniWeather.text = "☀️ 0°"
        if (isAutoLocationEnabled) {
            CoroutineScope(Dispatchers.IO).launch {
                val cityName = getCityNameLocally(lat.toDouble(), lon.toDouble())
                CoroutineScope(Dispatchers.Main).launch { tvLocation.text = "$cityName ➤" }
            }
        } else {
            tvLocation.text = "Текущее местоположение ➤"
        }
    }

    private fun getCityNameLocally(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) addresses[0].locality ?: "Локация" else "Локация"
        } catch (e: Exception) { "Локация" }
    }

    private fun requestLocation() {
        val locManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            tvCondition.text = "Поиск..."
            val last = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (last != null) {
                lat = last.latitude.toString(); lon = last.longitude.toString()
                fetchWeather()
            } else setDefaults()

            locManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    lat = loc.latitude.toString(); lon = loc.longitude.toString()
                    fetchWeather()
                }
                override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) { setDefaults() }
            }, null)
        } else { setDefaults() }
    }
}