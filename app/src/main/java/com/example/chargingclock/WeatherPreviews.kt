package com.example.chargingclock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview(showBackground = true, backgroundColor = 0xFF333333)
@Composable
fun WeatherIconsPreview() {
    // Используем Column с прокруткой, чтобы все иконки поместились
    Column(
        modifier = Modifier
            .background(Color(0xFF333333)) // Темный фон для контраста
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PreviewItem("Ясно (Clear)") {
            AnimatedClearIcon(modifier = Modifier.size(100.dp))
        }

        PreviewItem("Малооблачно (Partly Cloudy)") {
            AnimatedPartlyCloudyIcon(modifier = Modifier.size(100.dp))
        }

        PreviewItem("Пасмурно (Overcast)") {
            AnimatedOvercastIcon(modifier = Modifier.size(100.dp))
        }

        PreviewItem("Дождь (Rain)") {
            AnimatedRainIcon(modifier = Modifier.size(100.dp))
        }

        PreviewItem("Снег (Snow)") {
            AnimatedSnowIcon(modifier = Modifier.size(100.dp))
        }

        PreviewItem("Гроза (Storm)") {
            AnimatedStormIcon(modifier = Modifier.size(100.dp))
        }
    }
}

// Вспомогательная функция для подписей
@Composable
fun PreviewItem(title: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
        content()
    }
}