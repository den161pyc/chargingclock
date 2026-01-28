package com.example.chargingclock

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    // Theme & Layout
    private lateinit var rgThemeColor: RadioGroup
    private lateinit var rbThemeBlue: RadioButton
    private lateinit var rbThemeRed: RadioButton
    private lateinit var rbThemeGreen: RadioButton
    private lateinit var rbThemeYellow: RadioButton
    private lateinit var cbSplitMode: CheckBox
    private lateinit var rgSideContent: RadioGroup
    private lateinit var rbSideDate: RadioButton
    private lateinit var rbSideCalendar: RadioButton
    private lateinit var rbSideWeather: RadioButton
    private lateinit var rbSideMusic: RadioButton

    // Weather
    private lateinit var etYandexKey: EditText
    private lateinit var etOwmKey: EditText
    private lateinit var rgWeatherProvider: RadioGroup
    private lateinit var rbYandex: RadioButton
    private lateinit var rbOpenWeather: RadioButton

    // General
    private lateinit var cbShowBattery: CheckBox
    private lateinit var cbAutoBrightness: CheckBox
    private lateinit var cbNightFilter: CheckBox
    private lateinit var cbAutoLocation: CheckBox
    private lateinit var cbShowNextAlarm: CheckBox

    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()

        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        loadSettings(prefs)

        btnSave.setOnClickListener { saveSettings(prefs) }
        btnCancel.setOnClickListener { finish() }
    }

    private fun initViews() {
        // Theme
        rgThemeColor = findViewById(R.id.rgThemeColor)
        rbThemeBlue = findViewById(R.id.rbThemeBlue)
        rbThemeRed = findViewById(R.id.rbThemeRed)
        rbThemeGreen = findViewById(R.id.rbThemeGreen)
        rbThemeYellow = findViewById(R.id.rbThemeYellow)
        cbSplitMode = findViewById(R.id.cbSplitMode)
        rgSideContent = findViewById(R.id.rgSideContent)
        rbSideDate = findViewById(R.id.rbSideDate)
        rbSideCalendar = findViewById(R.id.rbSideCalendar)
        rbSideWeather = findViewById(R.id.rbSideWeather)
        rbSideMusic = findViewById(R.id.rbSideMusic)

        // Weather
        etYandexKey = findViewById(R.id.etYandexKey)
        etOwmKey = findViewById(R.id.etOwmKey)
        rgWeatherProvider = findViewById(R.id.rgWeatherProvider)
        rbYandex = findViewById(R.id.rbYandex)
        rbOpenWeather = findViewById(R.id.rbOpenWeather)

        // General
        cbShowBattery = findViewById(R.id.cbShowBattery)
        cbAutoBrightness = findViewById(R.id.cbAutoBrightness)
        cbNightFilter = findViewById(R.id.cbNightFilter)
        cbAutoLocation = findViewById(R.id.cbAutoLocation)
        cbShowNextAlarm = findViewById(R.id.cbShowNextAlarm)

        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun loadSettings(prefs: SharedPreferences) {
        // Theme Color (0=Blue, 1=Red, 2=Green, 3=Yellow)
        when (prefs.getInt("THEME_COLOR_ID", 0)) {
            1 -> rbThemeRed.isChecked = true
            2 -> rbThemeGreen.isChecked = true
            3 -> rbThemeYellow.isChecked = true
            else -> rbThemeBlue.isChecked = true
        }

        // Layout
        cbSplitMode.isChecked = prefs.getBoolean("IS_SPLIT_MODE", false)
        when (prefs.getInt("SIDE_CONTENT_MODE", 0)) {
            1 -> rbSideCalendar.isChecked = true
            2 -> rbSideWeather.isChecked = true
            3 -> rbSideMusic.isChecked = true
            else -> rbSideDate.isChecked = true
        }

        // Weather Provider
        if (prefs.getInt("WEATHER_PROVIDER_ID", 0) == 0) rbYandex.isChecked = true else rbOpenWeather.isChecked = true
        etYandexKey.setText(prefs.getString("YANDEX_API_KEY", ""))
        etOwmKey.setText(prefs.getString("OWM_API_KEY", ""))

        // General
        cbShowBattery.isChecked = prefs.getBoolean("SHOW_BATTERY_STATUS", true)
        cbAutoBrightness.isChecked = prefs.getBoolean("AUTO_BRIGHTNESS", false)
        cbNightFilter.isChecked = prefs.getBoolean("NIGHT_FILTER_ENABLED", false)
        cbAutoLocation.isChecked = prefs.getBoolean("AUTO_LOCATION", false)
        cbShowNextAlarm.isChecked = prefs.getBoolean("SHOW_NEXT_ALARM", false)
    }

    private fun saveSettings(prefs: SharedPreferences) {
        val editor = prefs.edit()

        // Theme
        val themeId = when (rgThemeColor.checkedRadioButtonId) {
            R.id.rbThemeRed -> 1
            R.id.rbThemeGreen -> 2
            R.id.rbThemeYellow -> 3
            else -> 0
        }
        editor.putInt("THEME_COLOR_ID", themeId)

        // Layout
        editor.putBoolean("IS_SPLIT_MODE", cbSplitMode.isChecked)
        val sideMode = when (rgSideContent.checkedRadioButtonId) {
            R.id.rbSideCalendar -> 1
            R.id.rbSideWeather -> 2
            R.id.rbSideMusic -> 3
            else -> 0
        }
        editor.putInt("SIDE_CONTENT_MODE", sideMode)

        // Weather
        editor.putInt("WEATHER_PROVIDER_ID", if (rbYandex.isChecked) 0 else 1)
        editor.putString("YANDEX_API_KEY", etYandexKey.text.toString().trim())
        editor.putString("OWM_API_KEY", etOwmKey.text.toString().trim())

        // General
        editor.putBoolean("SHOW_BATTERY_STATUS", cbShowBattery.isChecked)
        editor.putBoolean("AUTO_BRIGHTNESS", cbAutoBrightness.isChecked)
        editor.putBoolean("NIGHT_FILTER_ENABLED", cbNightFilter.isChecked)
        editor.putBoolean("AUTO_LOCATION", cbAutoLocation.isChecked)
        editor.putBoolean("SHOW_NEXT_ALARM", cbShowNextAlarm.isChecked)

        editor.apply()
        Toast.makeText(this, "Сохранено! Перезапустите для применения темы.", Toast.LENGTH_LONG).show()
        finish()
    }
}