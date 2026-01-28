package com.example.chargingclock

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarManager(
    private val context: Context,
    private val repository: CalendarRepository,
    private val layout: LinearLayout,
    private val tvMonthName: TextView,
    private val headerLayout: LinearLayout,
    private val gridLayout: GridLayout,
    private val tvEvent: TextView
) {

    fun updateEvents() {
        CoroutineScope(Dispatchers.Main).launch {
            val eventText = repository.getNextEvent()
            tvEvent.text = eventText ?: "No events"
            tvEvent.visibility = View.VISIBLE
        }
    }

    fun drawCalendar(textColor: Int, themeColor: Int, typeface: Typeface?) {
        if (layout.visibility != View.VISIBLE) return

        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        // Название месяца
        val monthStr = SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(calendar.time)
        tvMonthName.text = monthStr.substring(0, 1).uppercase() + monthStr.substring(1)
        tvMonthName.setTextColor(textColor)
        if (typeface != null) tvMonthName.typeface = typeface

        // Тень для месяца
        val prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val showShadow = prefs.getBoolean("SHOW_TEXT_SHADOW", true)
        val shadowColor = Color.parseColor("#80000000")
        tvMonthName.setShadowLayer(if (showShadow) 12f else 0f, if (showShadow) 4f else 0f, if (showShadow) 4f else 0f, shadowColor)

        // Заголовки дней недели
        headerLayout.removeAllViews()
        val weekDays = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        val density = context.resources.displayMetrics.density
        val cellSize = (40 * density).toInt()

        for (day in weekDays) {
            val tv = TextView(context).apply {
                text = day
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(Color.argb(150, Color.red(textColor), Color.green(textColor), Color.blue(textColor)))
                layoutParams = LinearLayout.LayoutParams(cellSize, ViewGroup.LayoutParams.WRAP_CONTENT)
                if (typeface != null) this.typeface = typeface
                setShadowLayer(if (showShadow) 8f else 0f, if (showShadow) 2f else 0f, if (showShadow) 2f else 0f, shadowColor)
            }
            headerLayout.addView(tv)
        }

        // Сетка дней
        gridLayout.removeAllViews()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        var dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        if (dayOfWeek == 0) dayOfWeek = 7 // Воскресенье должно быть 7-м

        // Пустые ячейки в начале
        for (i in 0 until dayOfWeek - 1) {
            val emptyView = View(context).apply {
                layoutParams = GridLayout.LayoutParams().apply { width = cellSize; height = cellSize }
            }
            gridLayout.addView(emptyView)
        }

        // Дни месяца
        for (i in 1..daysInMonth) {
            val tvDay = TextView(context).apply {
                text = i.toString()
                gravity = Gravity.CENTER
                textSize = 16f
                layoutParams = GridLayout.LayoutParams().apply { width = cellSize; height = cellSize }
                if (typeface != null) this.typeface = typeface
                setShadowLayer(if (showShadow) 8f else 0f, if (showShadow) 2f else 0f, if (showShadow) 2f else 0f, shadowColor)
            }

            if (i == currentDay) {
                val circle = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(themeColor)
                }
                tvDay.background = circle
                tvDay.setTextColor(Color.WHITE)
                if (typeface == null) tvDay.typeface = Typeface.DEFAULT_BOLD
            } else {
                tvDay.setTextColor(textColor)
                tvDay.background = null
            }
            gridLayout.addView(tvDay)
        }
    }
}