package com.example.chargingclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class PowerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        val prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val isBgModeEnabled = prefs.getBoolean("BG_MODE_ENABLED", false)

        if (!isBgModeEnabled) return

        if (action == Intent.ACTION_POWER_CONNECTED) {
            // WakeLock для пробуждения процессора и экрана (важно для Android 8)
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "ChargingClock:LaunchWakeLock"
            )
            wakeLock.acquire(5000)

            try {
                val i = Intent(context, MainActivity::class.java)
                // Флаги для корректного запуска Activity
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)

                context.startActivity(i)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}