package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class OsExtraColors(
    val border: Color,
    val muted: Color,
    val coral: Color,
    val gold: Color,
    val success: Color,
    val danger: Color,
    val card: Color,
    val primaryDark: Color
)

val LocalOsExtraColors = staticCompositionLocalOf {
    OsExtraColors(
        border = OsBorderLight,
        muted = OsMutedLight,
        coral = OsCoral,
        gold = OsAccentGold,
        success = OsSuccess,
        danger = OsDanger,
        card = OsCardLight,
        primaryDark = OsPrimaryDark
    )
}

private val OsLightColorScheme = lightColorScheme(
    primary = OsPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EDE7),
    onPrimaryContainer = OsPrimaryDark,
    secondary = OsCoral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE5DD),
    onSecondaryContainer = OsCoralDark,
    tertiary = OsAccentGold,
    onTertiary = Color.White,
    background = OsBgLight,
    onBackground = OsInkLight,
    surface = OsCardLight,
    onSurface = OsInkLight,
    surfaceVariant = Color(0xFFF2EFE7),
    onSurfaceVariant = OsMutedLight,
    outline = OsBorderLight,
    error = OsDanger,
    onError = Color.White
)

private val OsDarkColorScheme = darkColorScheme(
    primary = OsPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A3F34),
    onPrimaryContainer = Color(0xFFBFE7DC),
    secondary = OsCoral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A1D12),
    onSecondaryContainer = Color(0xFFFFD8CC),
    tertiary = OsAccentGold,
    onTertiary = Color.Black,
    background = OsBgDark,
    onBackground = OsInkDark,
    surface = OsCardDark,
    onSurface = OsInkDark,
    surfaceVariant = Color(0xFF1D3A33),
    onSurfaceVariant = OsMutedDark,
    outline = OsBorderDark,
    error = OsDanger,
    onError = Color.White
)

private val OsShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun OsMedicineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmharic: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OsDarkColorScheme else OsLightColorScheme
    val extras = if (darkTheme) {
        OsExtraColors(
            border = OsBorderDark,
            muted = OsMutedDark,
            coral = OsCoral,
            gold = OsAccentGold,
            success = OsSuccess,
            danger = OsDanger,
            card = OsCardDark,
            primaryDark = OsPrimaryDark
        )
    } else {
        OsExtraColors(
            border = OsBorderLight,
            muted = OsMutedLight,
            coral = OsCoral,
            gold = OsAccentGold,
            success = OsSuccess,
            danger = OsDanger,
            card = OsCardLight,
            primaryDark = OsPrimaryDark
        )
    }

    CompositionLocalProvider(LocalOsExtraColors provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildOsTypography(isAmharic),
            shapes = OsShapes,
            content = content
        )
    }
}
