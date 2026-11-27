package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ZenixDarkColorScheme = darkColorScheme(
  primary = ZenixCrimson,
  onPrimary = Color.White,
  primaryContainer = Color(0xFF4A0710),
  onPrimaryContainer = Color(0xFFFFDAD6),
  secondary = ZenixCoral,
  onSecondary = Color(0xFF1A0608),
  secondaryContainer = Color(0xFF331B24),
  onSecondaryContainer = Color(0xFFFFDAD9),
  tertiary = ZenixVioletAccent,
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFF281D45),
  onTertiaryContainer = Color(0xFFEADDFF),
  background = ZenixObsidian,
  onBackground = ZenixTextPrimary,
  surface = ZenixSurfaceDark,
  onSurface = ZenixTextPrimary,
  surfaceVariant = ZenixSurfaceElevated,
  onSurfaceVariant = ZenixTextSecondary,
  surfaceContainerLowest = ZenixObsidian,
  surfaceContainerLow = ZenixSurfaceDark,
  surfaceContainer = ZenixSurfaceElevated,
  surfaceContainerHigh = ZenixSurfaceHigh,
  outline = ZenixOutline,
  outlineVariant = ZenixOutlineSubtle,
  error = Color(0xFFFF6B6B),
  onError = Color(0xFF2B0002),
)

val ZenixShapes = Shapes(
  extraSmall = RoundedCornerShape(6.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(20.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ZenixTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = ZenixDarkColorScheme,
    typography = ZenixTypography,
    shapes = ZenixShapes,
    content = content,
  )
}
