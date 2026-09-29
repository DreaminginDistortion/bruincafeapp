package com.bruincafe.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CafeColorScheme = lightColorScheme(
    primary = CafeGreen,
    secondary = CafeGold,
    tertiary = CafeGold,
    background = CafeSurface,
    surface = CafeSurface,
    surfaceVariant = CafeCream,
    error = CafeError
)

/**
 * Single theming entry point -- change colors in Color.kt, not here, and every
 * screen in the app updates. (Equivalent to editing an Asset Catalog / a
 * central `Color` extension in a SwiftUI app.)
 */
@Composable
fun BruinCafeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CafeColorScheme,
        content = content
    )
}
