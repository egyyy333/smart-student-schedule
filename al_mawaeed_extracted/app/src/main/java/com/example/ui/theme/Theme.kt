package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = CyanPrimary,
    secondary = CyanSecondary,
    tertiary = GoldAccent80,
    background = SlateDarkBg,
    surface = SlateSurface,
    surfaceVariant = SlateSurfaceVariant,
    onPrimary = SlateDarkBg,
    onSecondary = SlateDarkBg,
    onTertiary = SlateDarkBg,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = LightTextSecondary
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DarkTeal,
    secondary = MediumTeal,
    tertiary = GoldAccent,
    background = Color(0xFFF5F8F6),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color(0xFF332200),
    onBackground = Color(0xFF0C1412),
    onSurface = Color(0xFF0C1412)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamicColor to ensure our custom theme displays
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
