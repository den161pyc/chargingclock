package com.example.chargingclock

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.CalendarView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.FileDescriptor
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener {

    private var currentThemeId = 0
    private var lat = "55.75"
    private var lon = "37.62"

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var ivBackground: ImageView
    private lateinit var batteryStatusText: TextView
    private lateinit var btnSettings: ImageButton

    private lateinit var containerSide: FrameLayout
    private lateinit var tsClock: TextSwitcher
    private lateinit var textDateSmall: TextClock
    private lateinit var textDateLarge: TextClock
    private lateinit var calendarView: CalendarView

    private lateinit var weatherLayout: LinearLayout
    private lateinit var tvWeatherTemp: TextView
    private lateinit var tvWeatherCondition: TextView
    private lateinit var tvWeatherIcon: TextView

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var locationManager: LocationManager

    private var isAutoBrightnessEnabled = false
    private var isRedTintEnabled = false
    private var isAutoLocationEnabled = false

    private var isNightModeActive = false

    private val handler = Handler(Looper.getMainLooper())
    private val weatherUpdateRunnable = object : Runnable {
        override fun run() {
            fetchWeather()
            handler.postDelayed(this, 3600000)
        }
    }

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateClockTime()
        }
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    batteryStatusText.text = "Заряжается"
                    if (!isNightModeActive || !isRedTintEnabled) {
                        batteryStatusText.setTextColor(Color.GREEN)
                    } else {
                        batteryStatusText.setTextColor(Color.RED)
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    batteryStatusText.text = "Работа от батареи"
                    batteryStatusText.setTextColor(Color.RED)

                    val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                    if (prefs.getBoolean("BG_MODE_ENABLED", false)) {
                        finishAffinity()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        currentThemeId = prefs.getInt("THEME_COLOR_ID", 0)
        setTheme(getThemeResId(currentThemeId))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setContentView(R.layout.activity_main)

        rootLayout = findViewById(R.id.rootLayout)
        ivBackground = findViewById(R.id.ivBackground)
        batteryStatusText = findViewById(R.id.batteryStatus)
        btnSettings = findViewById(R.id.btnSettings)

        tsClock = findViewById(R.id.tsClock)
        tsClock.setFactory {
            val t = TextView(this)
            t.gravity = Gravity.CENTER
            t.includeFontPadding = false
            t.setTextColor(Color.WHITE)
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 100f)
            t.typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
            t
        }
        tsClock.inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        tsClock.outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)

        containerSide = findViewById(R.id.containerSide)
        textDateSmall = findViewById(R.id.textDateSmall)
        textDateLarge = findViewById(R.id.textDateLarge)
        calendarView = findViewById(R.id.calendarView)

        weatherLayout = findViewById(R.id.weatherLayout)
        tvWeatherTemp = findViewById(R.id.tvWeatherTemp)
        tvWeatherCondition = findViewById(R.id.tvWeatherCondition)
        tvWeatherIcon = findViewById(R.id.tvWeatherIcon)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(powerReceiver, filter)
        updateInitialBatteryStatus()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()

        val timeFilter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        registerReceiver(timeTickReceiver, timeFilter)
        updateClockTime()

        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

        if (prefs.getInt("THEME_COLOR_ID", 0) != currentThemeId) {
            recreate()
            return
        }

        isNightModeActive = false

        applyAppearance(prefs)

        isAutoBrightnessEnabled = prefs.getBoolean("AUTO_BRIGHTNESS", false)
        isRedTintEnabled = prefs.getBoolean("RED_TINT_ENABLED", false)
        isAutoLocationEnabled = prefs.getBoolean("AUTO_LOCATION", false)
        val isSplitMode = prefs.getBoolean("IS_SPLIT_MODE", false)
        val sideMode = prefs.getInt("SIDE_CONTENT_MODE", 0)

        val showBattery = prefs.getBoolean("SHOW_BATTERY_STATUS", true)
        batteryStatusText.visibility = if (showBattery) View.VISIBLE else View.GONE

        applyCustomFont(prefs)

        if (isAutoBrightnessEnabled) {
            lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        } else {
            sensorManager.unregisterListener(this)
            resetBrightnessAndFilter()
        }

        handler.removeCallbacks(weatherUpdateRunnable)

        if (isSplitMode) {
            containerSide.visibility = View.VISIBLE
            textDateSmall.visibility = View.GONE

            // --- ИЗМЕНЕНИЕ: 130f ВМЕСТО 150f ---
            setClockSize(130f)

            textDateLarge.visibility = View.GONE
            calendarView.visibility = View.GONE
            weatherLayout.visibility = View.GONE

            when (sideMode) {
                1 -> calendarView.visibility = View.VISIBLE
                2 -> {
                    weatherLayout.visibility = View.VISIBLE
                    if (isAutoLocationEnabled) requestLocationUpdate() else weatherUpdateRunnable.run()
                }
                else -> textDateLarge.visibility = View.VISIBLE
            }
        } else {
            containerSide.visibility = View.GONE
            val showDate = prefs.getBoolean("SHOW_CLOCK_DATE", true)
            if (showDate) {
                textDateSmall.visibility = View.VISIBLE
                setClockSize(200f)
            } else {
                textDateSmall.visibility = View.GONE
                setClockSize(250f)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(timeTickReceiver)
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(weatherUpdateRunnable)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isAutoBrightnessEnabled && event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0]
            val layoutParams = window.attributes

            if (lux < 5f) {
                val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                val nightLevel = prefs.getInt("NIGHT_BRIGHTNESS_LEVEL", 1)
                layoutParams.screenBrightness = if (nightLevel < 1) 0.01f else nightLevel / 100f

                if (!isNightModeActive) {
                    isNightModeActive = true
                    if (isRedTintEnabled) {
                        applyRedText()
                    }
                }
            } else {
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

                if (isNightModeActive) {
                    isNightModeActive = false
                    val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                    applyAppearance(prefs)
                }
            }
            window.attributes = layoutParams
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun resetBrightnessAndFilter() {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams

        if (isNightModeActive) {
            isNightModeActive = false
            val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
            applyAppearance(prefs)
        }
    }

    private fun applyRedText() {
        val color = Color.RED

        setClockColor(color)
        textDateSmall.setTextColor(color)
        textDateLarge.setTextColor(color)
        tvWeatherTemp.setTextColor(color)
        tvWeatherCondition.setTextColor(color)
        tvWeatherIcon.setTextColor(color)
        batteryStatusText.setTextColor(color)
    }

    private fun applyAppearance(prefs: android.content.SharedPreferences) {
        val textColorId = prefs.getInt("TEXT_COLOR_ID", 0)
        val color = when(textColorId) {
            1 -> Color.GREEN
            2 -> Color.CYAN
            3 -> Color.YELLOW
            4 -> Color.RED
            else -> Color.WHITE
        }

        setClockColor(color)
        textDateSmall.setTextColor(color)
        textDateLarge.setTextColor(color)
        tvWeatherTemp.setTextColor(color)
        tvWeatherCondition.setTextColor(color)
        tvWeatherIcon.setTextColor(color)

        val bgImageUri = prefs.getString("BG_IMAGE_URI", null)
        if (bgImageUri != null) {
            try {
                ivBackground.visibility = View.VISIBLE
                ivBackground.setImageURI(Uri.parse(bgImageUri))
                val alpha = prefs.getInt("BG_IMAGE_ALPHA", 255)
                ivBackground.imageAlpha = alpha
                rootLayout.setBackgroundColor(Color.BLACK)
            } catch (e: Exception) {
                ivBackground.visibility = View.GONE
                applyBackgroundColor(prefs)
            }
        } else {
            ivBackground.visibility = View.GONE
            applyBackgroundColor(prefs)
        }
    }

    private fun updateClockTime() {
        val format = if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        val currentTime = sdf.format(Date())
        val currentViewText = (tsClock.currentView as? TextView)?.text?.toString()
        if (currentViewText != currentTime) {
            tsClock.setText(currentTime)
        }

        // --- ИСПРАВЛЕНИЕ: Обновление календаря ---
        // Если календарь виден, обновляем его дату на текущую,
        // чтобы при наступлении полуночи кружок перепрыгнул на новый день.
        if (calendarView.visibility == View.VISIBLE) {
            calendarView.date = System.currentTimeMillis()
        }
    }

    private fun setClockSize(sizeSp: Float) {
        for (i in 0 until tsClock.childCount) {
            val v = tsClock.getChildAt(i) as TextView
            v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        }
    }

    private fun setClockColor(color: Int) {
        for (i in 0 until tsClock.childCount) {
            val v = tsClock.getChildAt(i) as TextView
            v.setTextColor(color)
        }
    }

    private fun setClockTypeface(tf: Typeface) {
        for (i in 0 until tsClock.childCount) {
            val v = tsClock.getChildAt(i) as TextView
            v.typeface = tf
        }
    }

    private fun applyBackgroundColor(prefs: android.content.SharedPreferences) {
        val bgColorId = prefs.getInt("BG_COLOR_ID", 0)
        val color = when(bgColorId) {
            1 -> Color.DKGRAY
            2 -> Color.parseColor("#0D47A1")
            3 -> Color.parseColor("#B71C1C")
            4 -> Color.parseColor("#1B5E20")
            else -> Color.BLACK
        }
        rootLayout.setBackgroundColor(color)
    }

    private fun getThemeResId(id: Int): Int {
        return when(id) {
            1 -> R.style.Theme_ChargingClock_Red
            2 -> R.style.Theme_ChargingClock_Green
            3 -> R.style.Theme_ChargingClock_Yellow
            4 -> R.style.Theme_ChargingClock_Purple
            else -> R.style.Theme_ChargingClock_Blue
        }
    }

    private fun applyCustomFont(prefs: android.content.SharedPreferences) {
        val fontUriString = prefs.getString("CUSTOM_FONT_URI", null)
        if (fontUriString != null) {
            try {
                val uri = Uri.parse(fontUriString)
                val pfd = contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val fd: FileDescriptor = pfd.fileDescriptor
                    val typeface = Typeface.Builder(fd).build()
                    pfd.close()
                    setClockTypeface(typeface)
                    textDateSmall.typeface = typeface
                    textDateLarge.typeface = typeface
                    tvWeatherTemp.typeface = typeface
                    tvWeatherCondition.typeface = typeface
                    tvWeatherIcon.typeface = typeface
                    batteryStatusText.typeface = typeface
                }
            } catch (e: Exception) { resetFonts() }
        } else { resetFonts() }
    }

    private fun resetFonts() {
        val defaultTypeface = Typeface.DEFAULT
        val clockTypeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        setClockTypeface(clockTypeface)
        textDateSmall.typeface = defaultTypeface
        textDateLarge.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        tvWeatherTemp.typeface = clockTypeface
        tvWeatherCondition.typeface = defaultTypeface
        tvWeatherIcon.typeface = defaultTypeface
        batteryStatusText.typeface = defaultTypeface
    }

    private fun requestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }
        tvWeatherCondition.text = "Поиск..."
        val lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (lastLocation != null) {
            lat = lastLocation.latitude.toString()
            lon = lastLocation.longitude.toString()
            weatherUpdateRunnable.run()
        }
        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, object : LocationListener {
            override fun onLocationChanged(location: Location) {
                lat = location.latitude.toString()
                lon = location.longitude.toString()
                fetchWeather()
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }, null)
    }

    private fun fetchWeather() {
        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("YANDEX_API_KEY", "")

        if (apiKey.isNullOrEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                tvWeatherCondition.text = "Введите ключ"
                tvWeatherTemp.text = "--"
                tvWeatherIcon.text = ""
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlString = "https://api.weather.yandex.ru/v2/forecast?lat=$lat&lon=$lon&lang=ru_RU"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("X-Yandex-Weather-Key", apiKey)

                if (connection.responseCode == 200) {
                    val data = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(data)
                    val fact = json.getJSONObject("fact")
                    val temp = fact.getInt("temp")
                    val condition = fact.getString("condition")
                    val conditionRu = translateCondition(condition)
                    val icon = getWeatherIcon(condition)

                    withContext(Dispatchers.Main) {
                        tvWeatherTemp.text = "${if(temp > 0) "+" else ""}$temp°"
                        tvWeatherCondition.text = conditionRu
                        tvWeatherIcon.text = icon
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (connection.responseCode == 403) tvWeatherCondition.text = "Ошибка ключа"
                        else tvWeatherCondition.text = "Err: ${connection.responseCode}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { tvWeatherCondition.text = "Нет сети" }
            }
        }
    }

    private fun getWeatherIcon(cond: String): String {
        return when(cond) {
            "clear" -> "☀️"
            "partly-cloudy" -> "⛅"
            "cloudy" -> "☁️"
            "overcast" -> "☁️"
            "drizzle" -> "🌦️"
            "light-rain" -> "🌧️"
            "rain" -> "🌧️"
            "moderate-rain" -> "🌧️"
            "heavy-rain" -> "⛈️"
            "showers" -> "☔"
            "wet-snow" -> "🌨️"
            "light-snow" -> "🌨️"
            "snow" -> "❄️"
            "hail" -> "🌨️"
            "thunderstorm" -> "⚡"
            else -> "🌡️"
        }
    }

    private fun translateCondition(cond: String): String {
        return when(cond) {
            "clear" -> "Ясно"
            "partly-cloudy" -> "Малооблачно"
            "cloudy" -> "Облачно с проясн."
            "overcast" -> "Пасмурно"
            "drizzle" -> "Морось"
            "light-rain" -> "Небольшой дождь"
            "rain" -> "Дождь"
            "moderate-rain" -> "Умеренный дождь"
            "heavy-rain" -> "Сильный дождь"
            "showers" -> "Ливень"
            "wet-snow" -> "Дождь со снегом"
            "light-snow" -> "Небольшой снег"
            "snow" -> "Снег"
            "hail" -> "Град"
            "thunderstorm" -> "Гроза"
            else -> cond
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdate()
        } else {
            weatherUpdateRunnable.run()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun updateInitialBatteryStatus() {
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        if (isCharging) {
            batteryStatusText.text = "Заряжается"
            batteryStatusText.setTextColor(Color.GREEN)
        } else {
            batteryStatusText.text = "Работа от батареи"
            batteryStatusText.setTextColor(Color.RED)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(powerReceiver)
    }
}