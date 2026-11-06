package com.verticalcut.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0E0F14)
val Surface = Color(0xFF171922)
val SurfaceHigh = Color(0xFF232634)
val Accent = Color(0xFF7CFFB2)
val Accent2 = Color(0xFFFF7A59)
val TextMain = Color(0xFFF3F4F8)
val TextDim = Color(0xFF9DA2B5)

private val scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Bg,
    secondary = Accent2,
    onSecondary = Bg,
    background = Bg,
    onBackground = TextMain,
    surface = Surface,
    onSurface = TextMain,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextDim,
)

@Composable
fun VerticalCutTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
