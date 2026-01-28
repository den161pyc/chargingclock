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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

@Composable
fun AnimatedOvercastIcon(
    modifier: Modifier = Modifier,
    iconColor: Color = Color.White,
    backgroundColor: Color = Color.Black,
    isAnimating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "overcast_anim")

    val backX by if (isAnimating) {
        infiniteTransition.animateFloat(-15f, 15f, infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "back")
    } else {
        rememberInfiniteTransition(label = "s").animateFloat(0f, 0f, infiniteRepeatable(tween(1)), "s")
    }

    val frontX by if (isAnimating) {
        infiniteTransition.animateFloat(10f, -10f, infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "front")
    } else {
        rememberInfiniteTransition(label = "s").animateFloat(0f, 0f, infiniteRepeatable(tween(1)), "s")
    }

    val currentBackX = if (isAnimating) backX else 0f
    val currentFrontX = if (isAnimating) frontX else 0f

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = width * 0.03f
        val backCloudColor = iconColor.copy(alpha = 0.6f)

        translate(left = currentBackX + width * 0.1f, top = -height * 0.15f) {
            scale(scaleX = 0.8f, scaleY = 0.8f, pivot = Offset(width / 2, height / 2)) {
                val backCloudPath = Path().apply { addCloudShape(width, height) }
                drawPath(path = backCloudPath, color = backCloudColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }

        translate(left = currentFrontX, top = height * 0.05f) {
            val frontCloudPath = Path().apply { addCloudShape(width, height) }
            drawPath(path = frontCloudPath, color = backgroundColor)
            drawPath(path = frontCloudPath, color = iconColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}