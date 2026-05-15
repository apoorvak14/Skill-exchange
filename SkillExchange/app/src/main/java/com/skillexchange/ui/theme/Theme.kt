package com.skillexchange.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF2F6F4E),
    secondary = Color(0xFFB85734),
    tertiary = Color(0xFF3E6D8E),
    background = Color(0xFFF8F7F1),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1E2520),
    onSurface = Color(0xFF1E2520)
)

@Composable
fun SkillExchangeTheme(content: @Composable () -> Unit) {
    val ignored = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
