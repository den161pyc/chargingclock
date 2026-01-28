package com.example.chargingclock

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedClearIcon(
    modifier: Modifier = Modifier,
    iconColor: Color = Color.White,
    isAnimating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "clear_anim")

    // If not animating, we use a static 0f value
    val rotation by if (isAnimating) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = "sun_rot"
        )
    } else {
        rememberInfiniteTransition(label = "static").animateFloat(0f, 0f, infiniteRepeatable(tween(1)), "static")
    }

    // Explicitly select the value
    val currentRotation = if (isAnimating) rotation else 0f

    Canvas(modifier = modifier) {
        val width = size.width
        val center = Offset(width / 2, size.height / 2)
        val strokeWidth = width * 0.03f
        val sunRadius = width * 0.25f
        val rayLength = width * 0.12f
        val rayOffset = width * 0.05f

        rotate(currentRotation, center) {
            drawCircle(color = iconColor, radius = sunRadius, center = center, style = Stroke(strokeWidth))
            for (i in 0 until 8) {
                val angle = Math.toRadians((i * 45).toDouble())
                val start = center + Offset(
                    (sunRadius + rayOffset) * cos(angle).toFloat(),
                    (sunRadius + rayOffset) * sin(angle).toFloat()
                )
                val end = center + Offset(
                    (sunRadius + rayOffset + rayLength) * cos(angle).toFloat(),
                    (sunRadius + rayOffset + rayLength) * sin(angle).toFloat()
                )
                drawLine(color = iconColor, start = start, end = end, strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
        }
    }
}