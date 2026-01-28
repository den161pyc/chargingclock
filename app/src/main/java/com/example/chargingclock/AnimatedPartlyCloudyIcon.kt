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
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedPartlyCloudyIcon(
    modifier: Modifier = Modifier,
    iconColor: Color = Color.White,
    backgroundColor: Color = Color.Black,
    isAnimating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "partly_anim")

    val rotation by if (isAnimating) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart), label = "sun"
        )
    } else {
        rememberInfiniteTransition(label = "static").animateFloat(0f, 0f, infiniteRepeatable(tween(1)), "static")
    }

    val currentRotation = if (isAnimating) rotation else 0f

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = width * 0.03f

        // Sun Geometry
        val sunRadius = width * 0.15f
        val rayLength = width * 0.07f
        val rayOffset = width * 0.04f
        val sunCenter = Offset(width * 0.70f, height * 0.30f)

        // Draw Sun (Background)
        rotate(degrees = currentRotation, pivot = sunCenter) {
            drawCircle(color = iconColor, radius = sunRadius, center = sunCenter, style = Stroke(width = strokeWidth))
            for (i in 0 until 8) {
                val angleRad = Math.toRadians((i * 45).toDouble())
                val startX = sunCenter.x + (sunRadius + rayOffset) * cos(angleRad).toFloat()
                val startY = sunCenter.y + (sunRadius + rayOffset) * sin(angleRad).toFloat()
                val endX = sunCenter.x + (sunRadius + rayOffset + rayLength) * cos(angleRad).toFloat()
                val endY = sunCenter.y + (sunRadius + rayOffset + rayLength) * sin(angleRad).toFloat()
                drawLine(color = iconColor, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
        }

        // Draw Cloud (Foreground)
        val cloudPath = Path().apply { addCloudShape(width, height) }
        drawPath(path = cloudPath, color = backgroundColor)
        drawPath(path = cloudPath, color = iconColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}