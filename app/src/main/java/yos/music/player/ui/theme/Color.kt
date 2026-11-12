package yos.music.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import yos.music.player.data.libraries.SettingsLibrary

// Premium, refined music accent: vibrant Rose-Coral
val primary = Color(0xFFFF2D55)
val primaryDark = Color(0xFFFF3358)

val headline = Color(0xFF88878E)
val headlineDark = Color(0xFF9898A2)

// Luxury Obsidian Velvet Dark Base (deep, warm, atmospheric depth)
val background = Color.White
val backgroundDark = Color(0xFF0C0D14)

val surfaceDark = Color(0xFF141622)
val surfaceContainerDark = Color(0xFF1C1E2C)
val surfaceElevatedDark = Color(0xFF242738)

val textPrimaryDark = Color(0xFFF7F7FA)
val textSecondaryDark = Color(0xFFA0A0AC)
val textTertiaryDark = Color(0xFF6B6C7A)

val glassBackgroundDark = Color(0xCC161824)
val glassBorderDark = Color(0x1AFFFFFF)

val settingBack = Color(0xFFF2F1F6)
val settingBackDark = Color(0xFF0C0D14)
val settingContainerBack = Color.White
val settingContainerBackDark = Color(0xFF161824)

@Composable
infix fun Color.withNight(nightColor: Color): Color {
    return if (isFlamingoInDarkMode()) nightColor else this
}

@Composable
fun isFlamingoInDarkMode(): Boolean {
    return if (SettingsLibrary.CustomTheme == "Auto") isSystemInDarkTheme() else SettingsLibrary.CustomTheme == "Dark"
}