package com.example.chargingclock

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val switchAutoBrightness = findViewById<Switch>(R.id.switchAutoBrightness)
        val btnBack = findViewById<Button>(R.id.btnBack)

        // Элементы управления режимами
        val rgDisplayMode = findViewById<RadioGroup>(R.id.rgDisplayMode)
        val rbModeClock = findViewById<RadioButton>(R.id.rbModeClock)
        val rbModeSplit = findViewById<RadioButton>(R.id.rbModeSplit)

        val containerSplitOptions = findViewById<LinearLayout>(R.id.containerSplitOptions)
        val rgSideContent = findViewById<RadioGroup>(R.id.rgSideContent)
        val rbSideDate = findViewById<RadioButton>(R.id.rbSideDate)
        val rbSideCalendar = findViewById<RadioButton>(R.id.rbSideCalendar)

        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

        // 1. Загрузка Auto Brightness
        switchAutoBrightness.isChecked = prefs.getBoolean("AUTO_BRIGHTNESS", false)
        switchAutoBrightness.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("AUTO_BRIGHTNESS", isChecked).apply()
        }

        // 2. Загрузка Display Mode (Clock=0, Split=1)
        val isSplit = prefs.getBoolean("IS_SPLIT_MODE", false)
        if (isSplit) rbModeSplit.isChecked = true else rbModeClock.isChecked = true

        // Управление видимостью поднастроек
        containerSplitOptions.visibility = if (isSplit) View.VISIBLE else View.GONE

        rgDisplayMode.setOnCheckedChangeListener { _, checkedId ->
            val splitSelected = (checkedId == R.id.rbModeSplit)
            prefs.edit().putBoolean("IS_SPLIT_MODE", splitSelected).apply()
            containerSplitOptions.visibility = if (splitSelected) View.VISIBLE else View.GONE
        }

        // 3. Загрузка Side Content (Date=0, Calendar=1)
        val isCalendar = prefs.getBoolean("IS_CALENDAR_SIDE", false)
        if (isCalendar) rbSideCalendar.isChecked = true else rbSideDate.isChecked = true

        rgSideContent.setOnCheckedChangeListener { _, checkedId ->
            val calendarSelected = (checkedId == R.id.rbSideCalendar)
            prefs.edit().putBoolean("IS_CALENDAR_SIDE", calendarSelected).apply()
        }

        btnBack.setOnClickListener { finish() }
    }
}