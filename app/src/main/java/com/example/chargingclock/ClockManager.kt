package com.example.chargingclock

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.format.DateFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.AnimationUtils
import android.widget.TextSwitcher
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockManager(
    private val context: Context,
    private val tsHour1: TextSwitcher,
    private val tsHour2: TextSwitcher,
    private val tsMinute1: TextSwitcher,
    private val tsMinute2: TextSwitcher,
    private val tvSeparator: TextView
) {

    fun setupSwitchers() {
        val switchers = listOf(tsHour1, tsHour2, tsMinute1, tsMinute2)
        val inAnim = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
        val outAnim = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)

        for (ts in switchers) {
            ts.setFactory {
                TextView(context).apply {
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 100f)
                    typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
                    fontFeatureSettings = "tnum"
                }
            }
            ts.inAnimation = inAnim
            ts.outAnimation = outAnim
        }
    }

    fun updateTime() {
        val format = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        val timeString = sdf.format(Date())
        val parts = timeString.split(":") // ["12", "45"]

        if (parts.size == 2) {
            val hours = parts[0]
            val minutes = parts[1]

            if (hours.length == 2) {
                setSwitcherText(tsHour1, hours[0].toString())
                setSwitcherText(tsHour2, hours[1].toString())
            } else {
                setSwitcherText(tsHour1, "0") // Или "", если не нужен ведущий ноль
                setSwitcherText(tsHour2, hours)
            }

            if (minutes.length == 2) {
                setSwitcherText(tsMinute1, minutes[0].toString())
                setSwitcherText(tsMinute2, minutes[1].toString())
            }
        }
    }

    fun applyColor(color: Int) {
        val views = listOf(tsHour1, tsHour2, tsMinute1, tsMinute2)
        for (ts in views) {
            for (i in 0 until ts.childCount) {
                (ts.getChildAt(i) as TextView).setTextColor(color)
            }
        }
        tvSeparator.setTextColor(color)
    }

    fun applySize(sizeSp: Float) {
        val views = listOf(tsHour1, tsHour2, tsMinute1, tsMinute2)
        for (ts in views) {
            for (i in 0 until ts.childCount) {
                (ts.getChildAt(i) as TextView).setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            }
        }
        tvSeparator.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    fun applyFont(tf: Typeface) {
        val views = listOf(tsHour1, tsHour2, tsMinute1, tsMinute2)
        for (ts in views) {
            for (i in 0 until ts.childCount) {
                (ts.getChildAt(i) as TextView).apply {
                    typeface = tf
                    fontFeatureSettings = "tnum"
                }
            }
        }
        tvSeparator.typeface = tf
    }

    private fun setSwitcherText(switcher: TextSwitcher, text: String) {
        val current = (switcher.currentView as? TextView)?.text?.toString() ?: ""
        if (current != text) {
            switcher.setText(text)
        }
    }
}