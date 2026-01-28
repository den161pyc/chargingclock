package com.example.chargingclock

import android.content.Context
import android.graphics.Color
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView

// Функция расширения для конвертации dp в px
fun Context.dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

// Функция для настройки обрезки углов (BlurView)
fun setupBlurViewClipping(imageView: ImageView) {
    imageView.outlineProvider = ViewOutlineProvider.BACKGROUND
    imageView.clipToOutline = true
    imageView.setBackgroundResource(R.drawable.bg_panel_rounded)
}

// Функция для применения тени к списку TextView
fun applyTextShadowToViews(views: List<TextView>, isEnabled: Boolean) {
    val radius = if (isEnabled) 12f else 0f
    val dx = if (isEnabled) 4f else 0f
    val dy = if (isEnabled) 4f else 0f
    val color = Color.parseColor("#80000000")

    for (tv in views) {
        tv.setShadowLayer(radius, dx, dy, color)
    }
}