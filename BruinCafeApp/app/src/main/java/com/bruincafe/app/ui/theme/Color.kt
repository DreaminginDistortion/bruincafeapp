package com.bruincafe.app.ui.theme

import androidx.compose.ui.graphics.Color

// Brand colors from the printed HQ Bruin Café menu.
val CafeGreen = Color(0xFF2F5233)
val CafeGoldLight = Color(0xFFFFF3D6)
val CafeCream = Color(0xFFFBF0DA)
val CafeSurface = Color(0xFFFFFDF8)
val CafeInk = Color(0xFF1C1B1F)       // body text

// ACCESSIBILITY: the original brand gold (B8860B) is only ~3:1 against cream, which
// fails WCAG AA for text. Use CafeGoldDark for any gold TEXT or gold BACKGROUND with
// white text (about 6:1). CafeGold stays available for decoration only.
val CafeGold = Color(0xFFB8860B)
val CafeGoldDark = Color(0xFF7A5800)

val CafeError = Color(0xFFB3261E)

// High-contrast mode (settings screen): black on white, very dark green, heavy borders.
val HcBlack = Color(0xFF000000)
val HcWhite = Color(0xFFFFFFFF)
val HcGreen = Color(0xFF00290A)
val HcError = Color(0xFF8C0000)
