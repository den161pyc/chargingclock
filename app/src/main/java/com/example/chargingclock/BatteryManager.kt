package com.example.chargingclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.widget.TextView

class BatteryManager(
    private val context: Context,
    private val batteryStatusText: TextView
) {
    private var isNightMode = false
    private var nightColor = Color.GRAY

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    batteryStatusText.text = "Заряжается"
                    updateColor()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    batteryStatusText.text = "Работа от батареи"
                    batteryStatusText.setTextColor(Color.RED)

                    // ИСПРАВЛЕНИЕ: Используем this@BatteryManager.context вместо receiverContext
                    val prefs = this@BatteryManager.context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)

                    if (prefs.getBoolean("BG_MODE_ENABLED", false)) {
                        // Логика закрытия (если потребуется)
                        // (this@BatteryManager.context as? android.app.Activity)?.finishAffinity()
                    }
                }
            }
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(powerReceiver, filter)
        updateInitialStatus()
    }

    fun unregister() {
        try {
            context.unregisterReceiver(powerReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateNightMode(isActive: Boolean, color: Int) {
        isNightMode = isActive
        nightColor = color
        updateColor()
    }

    private fun updateColor() {
        // Здесь используем context класса, он не null
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        if (isCharging) {
            if (!isNightMode) {
                batteryStatusText.setTextColor(Color.GREEN)
            } else {
                batteryStatusText.setTextColor(nightColor)
            }
        } else {
            batteryStatusText.setTextColor(Color.RED)
        }
    }

    private fun updateInitialStatus() {
        updateColor()
    }
}