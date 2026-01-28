package com.example.chargingclock

import androidx.compose.ui.graphics.Path

// Функция расширения для Path, рисующая форму облака
fun Path.addCloudShape(width: Float, height: Float) {
    moveTo(width * 0.25f, height * 0.70f) // Начало (левый нижний угол)

    // Нижняя линия
    lineTo(width * 0.75f, height * 0.70f)

    // Правая дуга
    cubicTo(
        width * 0.95f, height * 0.70f,
        width * 0.95f, height * 0.45f,
        width * 0.75f, height * 0.45f
    )

    // Верхняя центральная дуга
    cubicTo(
        width * 0.70f, height * 0.25f,
        width * 0.40f, height * 0.25f,
        width * 0.35f, height * 0.45f
    )

    // Левая дуга
    cubicTo(
        width * 0.15f, height * 0.45f,
        width * 0.15f, height * 0.70f,
        width * 0.25f, height * 0.70f
    )
    close()
}