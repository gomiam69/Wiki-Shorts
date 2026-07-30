package com.example.wikishorts.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WikiBlue = Color(0xFF3B82F6)
private val WikiBlueDark = Color(0xFF60A5FA)

private val DarkColors = darkColorScheme(
    primary = WikiBlueDark,
    secondary = WikiBlueDark,
    background = Color(0xFF000000),
    surface = Color(0xFF121212)
)

private val LightColors = lightColorScheme(
    primary = WikiBlue,
    secondary = WikiBlue,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF5F5F5)
)

@Composable
fun WikiShortsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
