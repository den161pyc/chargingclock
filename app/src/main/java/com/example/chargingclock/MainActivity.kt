package com.example.chargingclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.CalendarView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextClock
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var batteryStatusText: TextView
    private lateinit var btnSettings: ImageButton

    // Элементы нового интерфейса
    private lateinit var containerSide: FrameLayout
    private lateinit var textDateSmall: TextClock
    private lateinit var textDateLarge: TextClock
    private lateinit var calendarView: CalendarView

    // Переменные настроек
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var isAutoBrightnessEnabled = false

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    batteryStatusText.text = "Заряжается"
                    batteryStatusText.setTextColor(getColor(android.R.color.holo_green_light))
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    batteryStatusText.text = "Работа от батареи"
                    batteryStatusText.setTextColor(getColor(android.R.color.holo_red_light))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        // Инициализация View
        batteryStatusText = findViewById(R.id.batteryStatus)
        btnSettings = findViewById(R.id.btnSettings)

        containerSide = findViewById(R.id.containerSide)
        textDateSmall = findViewById(R.id.textDateSmall)
        textDateLarge = findViewById(R.id.textDateLarge)
        calendarView = findViewById(R.id.calendarView)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

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

        // Считываем все настройки
        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        isAutoBrightnessEnabled = prefs.getBoolean("AUTO_BRIGHTNESS", false)

        val isSplitMode = prefs.getBoolean("IS_SPLIT_MODE", false)
        val isCalendarSide = prefs.getBoolean("IS_CALENDAR_SIDE", false)

        // Применяем настройки авто-яркости
        if (isAutoBrightnessEnabled) {
            lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        } else {
            sensorManager.unregisterListener(this)
            resetBrightness()
        }

        // Применяем настройки интерфейса (Часы или Сплит)
        if (isSplitMode) {
            containerSide.visibility = View.VISIBLE
            textDateSmall.visibility = View.GONE // Прячем маленькую дату, так как есть большая справа

            // Выбираем, что показать справа
            if (isCalendarSide) {
                calendarView.visibility = View.VISIBLE
                textDateLarge.visibility = View.GONE
            } else {
                calendarView.visibility = View.GONE
                textDateLarge.visibility = View.VISIBLE
            }
        } else {
            // Режим "Только часы"
            containerSide.visibility = View.GONE
            textDateSmall.visibility = View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // --- Логика датчика света ---
    override fun onSensorChanged(event: SensorEvent?) {
        if (isAutoBrightnessEnabled && event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0]
            val layoutParams = window.attributes
            if (lux < 5f) {
                layoutParams.screenBrightness = 0.01f
            } else {
                layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            window.attributes = layoutParams
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun resetBrightness() {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams
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
            batteryStatusText.setTextColor(getColor(android.R.color.holo_green_light))
        } else {
            batteryStatusText.text = "Работа от батареи"
            batteryStatusText.setTextColor(getColor(android.R.color.holo_red_light))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(powerReceiver)
    }
}