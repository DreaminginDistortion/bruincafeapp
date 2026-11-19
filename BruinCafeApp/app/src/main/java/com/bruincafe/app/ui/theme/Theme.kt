package com.bruincafe.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Screens read this to add heavy borders when the customer turns on high contrast. */
val LocalHighContrast = staticCompositionLocalOf { false }

// Every screen takes its colors from these roles (never from raw constants), which is
// what lets the high-contrast switch restyle the whole app at once.
private val NormalScheme = lightColorScheme(
    primary = CafeGreen, onPrimary = Color.White,
    secondary = CafeGoldDark, onSecondary = Color.White,
    tertiary = CafeGoldDark,
    secondaryContainer = CafeGoldLight, onSecondaryContainer = CafeInk,
    background = CafeSurface, onBackground = CafeInk,
    surface = CafeSurface, onSurface = CafeInk,
    surfaceVariant = CafeCream, onSurfaceVariant = CafeInk,
    error = CafeError, onError = Color.White
)

private val HighContrastScheme = lightColorScheme(
    primary = HcGreen, onPrimary = HcWhite,
    secondary = HcBlack, onSecondary = HcWhite,
    tertiary = HcBlack,
    secondaryContainer = HcWhite, onSecondaryContainer = HcBlack,
    background = HcWhite, onBackground = HcBlack,
    surface = HcWhite, onSurface = HcBlack,
    surfaceVariant = HcWhite, onSurfaceVariant = HcBlack,
    outline = HcBlack,
    error = HcError, onError = HcWhite
)

/**
 * Single theming entry point. [textScale] multiplies the phone's own font size, so
 * the in-app "Large / Extra large" choice stacks on top of the system setting.
 * (Text is sized in sp everywhere, so system font scaling already works too.)
 */
@Composable
fun BruinCafeTheme(
    highContrast: Boolean = false,
    textScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalHighContrast provides highContrast,
        LocalDensity provides Density(density.density, density.fontScale * textScale)
    ) {
        MaterialTheme(
            colorScheme = if (highContrast) HighContrastScheme else NormalScheme,
            content = content
        )
    }
}
