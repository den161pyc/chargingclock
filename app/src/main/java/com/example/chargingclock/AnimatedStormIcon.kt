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
fun AnimatedStormIcon(
    modifier: Modifier = Modifier,
    iconColor: Color = Color.White,
    backgroundColor: Color = Color.Black,
    isAnimating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "storm_anim")

    val lightningAlpha by if (isAnimating) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 2000
                    0f at 0
                    0f at 800
                    1f at 850 // Вспышка
                    0f at 950
                    1f at 1000
                    0f at 1200
                    0f at 2000
                },
                repeatMode = RepeatMode.Restart
            ), label = "lightning_flash"
        )
    } else {
        // Если статика - показываем молнию постоянно (прозрачность 1)
        rememberInfiniteTransition("static").animateFloat(1f, 1f, infiniteRepeatable(tween(1)), "static")
    }

    val currentAlpha = lightningAlpha

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = width * 0.03f

        // Молния
        if (currentAlpha > 0f) {
            val lightningPath = Path().apply {
                moveTo(width * 0.55f, height * 0.7f)
                lineTo(width * 0.45f, height * 0.82f)
                lineTo(width * 0.52f, height * 0.82f)
                lineTo(width * 0.42f, height * 0.95f)
            }

            drawPath(
                path = lightningPath,
                color = iconColor.copy(alpha = currentAlpha),
                style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
            )
        }

        // Облако
        val cloudPath = Path().apply { addCloudShape(width, height) }
        drawPath(path = cloudPath, color = backgroundColor)
        drawPath(
            path = cloudPath,
            color = iconColor,
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
    }
}