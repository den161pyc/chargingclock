package com.example.chargingclock

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import android.view.WindowManager

class BrightnessManager(
    private val activity: Activity,
    private val onNightModeChanged: (Boolean) -> Unit // Callback когда нужно включить ночной режим
) : SensorEventListener {

    private val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private var brightnessAnimator: ValueAnimator? = null

    var isAutoBrightnessEnabled = false
    private var isNightModeActive = false

    fun start() {
        if (isAutoBrightnessEnabled) {
            lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        } else {
            stop()
            resetBrightness()
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        brightnessAnimator?.cancel()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isAutoBrightnessEnabled && event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0]
            if (lux < 5f) {
                if (!isNightModeActive) {
                    isNightModeActive = true
                    onNightModeChanged(true) // Сообщаем Activity

                    val prefs = activity.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                    val nightLevel = prefs.getInt("NIGHT_BRIGHTNESS_LEVEL", 25)
                    val target = if (nightLevel < 1) 0.01f else nightLevel / 100f
                    animateScreenBrightness(target)
                }
            } else {
                if (isNightModeActive) {
                    isNightModeActive = false
                    onNightModeChanged(false) // Сообщаем Activity

                    val sysBrightness = getSystemBrightness()
                    animateScreenBrightness(sysBrightness) {
                        // Сбрасываем оверрайд яркости окна
                        val params = activity.window.attributes
                        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        activity.window.attributes = params
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun animateScreenBrightness(targetValue: Float, onEnd: (() -> Unit)? = null) {
        val current = if (activity.window.attributes.screenBrightness < 0) getSystemBrightness() else activity.window.attributes.screenBrightness
        brightnessAnimator?.cancel()
        brightnessAnimator = ValueAnimator.ofFloat(current, targetValue).apply {
            duration = 1000
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                val layoutParams = activity.window.attributes
                layoutParams.screenBrightness = value
                activity.window.attributes = layoutParams
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { onEnd?.invoke() }
            })
            start()
        }
    }

    private fun getSystemBrightness(): Float {
        return try {
            val cur = Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            cur / 255f
        } catch (e: Exception) { 0.5f }
    }

    private fun resetBrightness() {
        val params = activity.window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.window.attributes = params
    }
}