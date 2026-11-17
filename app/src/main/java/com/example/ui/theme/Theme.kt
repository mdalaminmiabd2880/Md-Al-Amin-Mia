package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

fun parseBrandColor(hex: String, fallback: Color): Color {
    return try {
        val clean = hex.trim()
        Color(android.graphics.Color.parseColor(clean))
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun ByaktigatoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    primaryHex: String = "#0D4A3E",
    accentHex: String = "#C9962B",
    bengaliFont: BengaliFontOption = BengaliFontOption.HIND_SILIGURI,
    content: @Composable () -> Unit
) {
    val brandPrimary = parseBrandColor(primaryHex, EmeraldPrimary)
    val brandAccent = parseBrandColor(accentHex, GoldAccent)

    val lightPrimaryOn = if (brandPrimary.luminance() > 0.5f) Color(0xFF10201C) else Color.White
    val darkPrimary = Color(
        red = (brandPrimary.red * 0.45f + 0.55f).coerceIn(0f, 1f),
        green = (brandPrimary.green * 0.45f + 0.55f).coerceIn(0f, 1f),
        blue = (brandPrimary.blue * 0.45f + 0.55f).coerceIn(0f, 1f),
        alpha = 1f
    )

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = darkPrimary,
            onPrimary = Color(0xFF07221B),
            primaryContainer = brandPrimary,
            onPrimaryContainer = Color(0xFFE0F4EE),
            secondary = brandAccent,
            onSecondary = Color(0xFF281B00),
            secondaryContainer = Color(0xFF4A3709),
            onSecondaryContainer = GoldContainer,
            background = DarkBackground,
            onBackground = Color(0xFFEAF0ED),
            surface = DarkSurface,
            onSurface = Color(0xFFEAF0ED),
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = Color(0xFFB5C4BE),
            outline = Color(0xFF3E524C),
            error = Color(0xFFFFB4AB)
        )
    } else {
        lightColorScheme(
            primary = brandPrimary,
            onPrimary = lightPrimaryOn,
            primaryContainer = EmeraldContainer,
            onPrimaryContainer = EmeraldOnContainer,
            secondary = brandAccent,
            onSecondary = Color.White,
            secondaryContainer = GoldContainer,
            onSecondaryContainer = GoldOnContainer,
            background = IvoryBackground,
            onBackground = Color(0xFF13201C),
            surface = CrispCardSurface,
            onSurface = Color(0xFF13201C),
            surfaceVariant = WarmSurface,
            onSurfaceVariant = Color(0xFF475852),
            outline = Color(0xFFBCC7C2),
            error = DangerRed
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildAppTypography(bengaliFont),
        shapes = AppShapes,
        content = content
    )
}
