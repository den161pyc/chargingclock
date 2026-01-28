package com.example.chargingclock

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

// Класс данных остается прежним
data class WeatherData(
    val temp: Int,
    val condition: String, // Внутренний код для анимации (clear, rain...)
    val conditionRu: String, // Текст для отображения (Ясно, Дождь...)
    val iconEmoji: String,
    val cityName: String
)

class WeatherRepository(private val context: Context) {

    // providerId: 0 = Yandex, 1 = OpenWeatherMap
    suspend fun fetchWeather(lat: String, lon: String, apiKey: String, providerId: Int): WeatherData? {
        return withContext(Dispatchers.IO) {
            try {
                if (providerId == 0) {
                    fetchYandexWeather(lat, lon, apiKey)
                } else {
                    fetchOpenWeatherMap(lat, lon, apiKey)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // --- YANDEX LOGIC ---
    private fun fetchYandexWeather(lat: String, lon: String, apiKey: String): WeatherData? {
        val cityName = getCityName(lat.toDouble(), lon.toDouble())
        val url = URL("https://api.weather.yandex.ru/v2/forecast?lat=$lat&lon=$lon&lang=ru_RU")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("X-Yandex-Weather-Key", apiKey)

        if (connection.responseCode == 200) {
            val data = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(data)
            val fact = json.getJSONObject("fact")
            val temp = fact.getInt("temp")
            val condition = fact.getString("condition")

            return WeatherData(
                temp = temp,
                condition = condition,
                conditionRu = translateYandexCondition(condition),
                iconEmoji = getWeatherIconEmoji(condition),
                cityName = cityName
            )
        }
        return null
    }

    // --- OPENWEATHERMAP LOGIC ---
    private fun fetchOpenWeatherMap(lat: String, lon: String, apiKey: String): WeatherData? {
        // Запрос: метрическая система, русский язык
        val url = URL("https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric&lang=ru")
        val connection = url.openConnection() as HttpURLConnection

        if (connection.responseCode == 200) {
            val data = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(data)

            // Температура
            val main = json.getJSONObject("main")
            val temp = main.getDouble("temp").roundToInt() // Округляем до целого

            // Погода (массив, берем первый элемент)
            val weatherArray = json.getJSONArray("weather")
            val weatherObj = weatherArray.getJSONObject(0)
            val owmId = weatherObj.getInt("id") // ID погоды (800, 501 и т.д.)
            val description = weatherObj.getString("description") // "пасмурно", "легкий дождь"
            val cityNameResponse = json.getString("name") // OWM возвращает имя города

            // Маппинг для наших анимаций
            val mappedCondition = mapOpenWeatherCondition(owmId)

            // Формируем красивое описание (с большой буквы)
            val conditionRuCapitalized = description.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            return WeatherData(
                temp = temp,
                condition = mappedCondition,
                conditionRu = conditionRuCapitalized,
                iconEmoji = getWeatherIconEmoji(mappedCondition),
                cityName = cityNameResponse // Можно использовать ответ OWM или getCityName
            )
        }
        return null
    }

    // --- HELPERS ---

    private fun getCityName(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.locality ?: address.subAdminArea ?: address.adminArea ?: "Location"
            } else "Location"
        } catch (e: Exception) {
            "Location"
        }
    }

    // Преобразование кодов OWM в наши строки (clear, rain, snow...)
    private fun mapOpenWeatherCondition(id: Int): String {
        return when (id) {
            in 200..232 -> "thunderstorm" // Гроза
            in 300..321 -> "drizzle"      // Морось
            in 500..531 -> "rain"         // Дождь
            in 600..622 -> "snow"         // Снег
            in 701..781 -> "cloudy"       // Туман/Мгла (считаем как облачно для анимации)
            800 -> "clear"                // Ясно
            801 -> "partly-cloudy"        // Малооблачно (few clouds)
            802 -> "partly-cloudy"        // Переменная облачность (scattered clouds)
            803, 804 -> "overcast"        // Пасмурно (broken/overcast clouds)
            else -> "clear"
        }
    }

    private fun getWeatherIconEmoji(cond: String): String {
        return when (cond) {
            "clear" -> "☀️"; "partly-cloudy" -> "⛅"; "cloudy" -> "☁️"; "overcast" -> "☁️"
            "drizzle" -> "🌦️"; "light-rain" -> "🌧️"; "rain" -> "🌧️"; "moderate-rain" -> "🌧️"
            "heavy-rain" -> "⛈️"; "showers" -> "☔"; "wet-snow" -> "🌨️"; "light-snow" -> "🌨️"
            "snow" -> "❄️"; "hail" -> "🌨️"; "thunderstorm" -> "⚡"; else -> "🌡️"
        }
    }

    private fun translateYandexCondition(cond: String): String {
        return when (cond) {
            "clear" -> "Ясно"; "partly-cloudy" -> "Малооблачно"; "cloudy" -> "Облачно с проясн."
            "overcast" -> "Пасмурно"; "drizzle" -> "Морось"; "light-rain" -> "Небольшой дождь"
            "rain" -> "Дождь"; "moderate-rain" -> "Дождь"; "heavy-rain" -> "Ливень"
            "showers" -> "Ливень"; "wet-snow" -> "Мокрый снег"; "light-snow" -> "Снег"
            "snow" -> "Снег"; "hail" -> "Град"; "thunderstorm" -> "Гроза"; else -> cond
        }
    }
}