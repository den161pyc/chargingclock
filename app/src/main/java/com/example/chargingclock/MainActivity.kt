package com.example.chargingclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var batteryStatusText: TextView

    // Приемник событий подключения/отключения питания
    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    batteryStatusText.text = "Заряжается"
                    batteryStatusText.setTextColor(getColor(android.R.color.holo_green_light))
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    // Опционально: закрыть приложение, если сняли с зарядки
                    // finish()
                    batteryStatusText.text = "Работа от батареи"
                    batteryStatusText.setTextColor(getColor(android.R.color.holo_red_light))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Не выключать экран
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)
        batteryStatusText = findViewById(R.id.batteryStatus)

        // Регистрируем слушатель питания
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(powerReceiver, filter)

        // Обновим статус батареи сразу при запуске
        updateInitialBatteryStatus()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    // Если пользователь коснулся экрана, панели могут появиться.
    // Скрываем их снова при потере фокуса или таймере, но проще при клике.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    // Функция для скрытия навигации и статус бара (Full Screen)
    private fun hideSystemUI() {
        // Используем старый добрый метод флагов, который работает и на Android 8, и на 14
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