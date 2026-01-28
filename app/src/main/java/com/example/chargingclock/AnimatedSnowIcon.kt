package com.example.chargingclock

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin

@Composable
fun AnimatedSnowIcon(
    modifier: Modifier = Modifier,
    iconColor: Color = Color.White,
    backgroundColor: Color = Color.Black,
    isAnimating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "snow_anim")

    val snowProgress by if (isAnimating) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = "snow_fall"
        )
    } else {
        // Фиксируем снежинки в середине
        rememberInfiniteTransition("static").animateFloat(0.5f, 0.5f, infiniteRepeatable(tween(1)), "static")
    }

    val currentProgressVal = snowProgress

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = width * 0.03f

        // Облако
        val cloudPath = Path().apply { addCloudShape(width, height) }
        drawPath(path = cloudPath, color = backgroundColor)
        drawPath(path = cloudPath, color = iconColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))

        // Снежинки (Рисуем всегда)
        val flakesCount = 3
        val startY = height * 0.75f
        val endY = height * 0.98f

        for (i in 0 until flakesCount) {
            val shift = if (isAnimating) i / 3f else i * 0.2f // Разный сдвиг для статики
            val progress = (currentProgressVal + shift) % 1f

            val y = startY + (endY - startY) * progress

            // Если анимация выключена, убираем синус (качание), просто рисуем точки
            val sway = if (isAnimating) sin(progress * 2 * Math.PI).toFloat() else 0f
            val xBase = width * (0.35f + i * 0.15f)
            val x = xBase + (width * 0.02f) * sway

            val alpha = if (isAnimating && progress > 0.8f) (1f - progress) * 5f else 1f

            drawCircle(
                color = iconColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = width * 0.02f,
                center = Offset(x, y)
            )
        }
    }
}