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

@Composable
fun AnimatedRainIcon(
    modifier: Modifier = Modifier,
    iconColor: Color = Color.White,
    backgroundColor: Color = Color.Black,
    isAnimating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rain_anim")

    // Если анимация включена, rainOffsetY меняется от 0 до 1.
    // Если выключена, фиксируем на 0.5 (середина падения), чтобы капли были видны.
    val rainOffsetY by if (isAnimating) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = "rain_drop"
        )
    } else {
        rememberInfiniteTransition("static").animateFloat(0.5f, 0.5f, infiniteRepeatable(tween(1)), "static")
    }

    // Присваиваем значение
    val currentRainOffset = rainOffsetY

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = width * 0.03f

        // 1. Рисуем облако (Всегда)
        val cloudPath = Path().apply { addCloudShape(width, height) }
        drawPath(path = cloudPath, color = backgroundColor)
        drawPath(
            path = cloudPath,
            color = iconColor,
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )

        // 2. Рисуем капли (ВСЕГДА, но они движутся только если currentRainOffset меняется)
        val dropStartX = width * 0.3f
        val dropSpacing = width * 0.15f
        val dropLength = height * 0.1f
        val maxDropY = height * 0.95f
        val startY = height * 0.75f

        for (i in 0..2) {
            val x = dropStartX + (i * dropSpacing)

            // Если статика, убираем сдвиг времени, чтобы капли выглядели аккуратно
            val timeShift = if (isAnimating) i * 0.33f else 0f
            val currentProgress = (currentRainOffset + timeShift) % 1f

            val y = startY + (maxDropY - startY) * currentProgress

            // При статике прозрачность полная, при анимации - исчезают внизу
            val alpha = if (isAnimating && currentProgress > 0.8f) (1f - currentProgress) * 5f else 1f

            drawLine(
                color = iconColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                start = Offset(x, y),
                end = Offset(x - (width * 0.02f), y + dropLength),
                strokeWidth = strokeWidth * 0.8f,
                cap = StrokeCap.Round
            )
        }
    }
}