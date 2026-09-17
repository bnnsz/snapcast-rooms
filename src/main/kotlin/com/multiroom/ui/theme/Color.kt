package com.multiroom.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Fluent / WinUI 3 tokens, as used by Windows 11 Settings and PowerToys.
// Names follow the WinUI theme resources so they can be checked against the
// WinUI Gallery.

// Light

val LightSolidBackgroundBase = Color(0xFFF3F3F3)     // page background
val LightLayerFill = Color(0xFFFBFBFB)               // content layer above page
val LightCardBackground = Color(0xFFFFFFFF)          // settings cards
val LightCardBackgroundSecondary = Color(0xFFF6F6F6) // expander content
val LightCardStroke = Color(0xFFEAEAEA)              // 1px card border
val LightDividerStroke = Color(0xFFEBEBEB)
val LightTextPrimary = Color(0xFF262626)
val LightTextSecondary = Color(0xFF747474)
val LightTextDisabled = Color(0xFF9D9D9D)
val LightSubtleFillSecondary = Color(0x0F000000)     // hover
val LightSubtleFillTertiary = Color(0x0A000000)      // pressed
val LightControlFill = Color(0xFFFFFFFF)
val LightControlStroke = Color(0xFFDCDCDC)

// Dark

val DarkSolidBackgroundBase = Color(0xFF202020)
val DarkLayerFill = Color(0xFF282828)
val DarkCardBackground = Color(0xFF2B2B2B)
val DarkCardBackgroundSecondary = Color(0xFF313131)
val DarkCardStroke = Color(0xFF2E2E2E)
val DarkDividerStroke = Color(0xFF2D2D2D)
val DarkTextPrimary = Color(0xFFEDEDED)
val DarkTextSecondary = Color(0xFFA8A8A8)
val DarkTextDisabled = Color(0xFF878787)
val DarkSubtleFillSecondary = Color(0x0FFFFFFF)
val DarkSubtleFillTertiary = Color(0x0AFFFFFF)
val DarkControlFill = Color(0xFF333333)
val DarkControlStroke = Color(0xFF3D3D3D)

// Icons
// A step back from text strength; Material's strokes are heavier than Segoe
// Fluent Icons.

val LightIconTint = Color(0xFF3D3D3D)
val DarkIconTint = Color(0xFFCCCCCC)

// Accent
// Windows default accent; dark themes use Light2, as Windows does.

val AccentDefault = Color(0xFF0078D4)
val AccentLight2 = Color(0xFF4CC2FF)
val AccentDark1 = Color(0xFF005EB7)

// System status

val SystemSuccess = Color(0xFF0F7B0F)
val SystemSuccessDark = Color(0xFF6CCB5F)
val SystemCaution = Color(0xFF9D5D00)
val SystemCautionDark = Color(0xFFFCE100)
val SystemCritical = Color(0xFFC42B1C)
val SystemCriticalDark = Color(0xFFFF99A4)

// Material3 mappings

val FluentLightColorScheme = lightColorScheme(
    primary = AccentDefault,
    onPrimary = Color.White,
    primaryContainer = AccentDefault.copy(alpha = 0.10f),
    onPrimaryContainer = AccentDark1,
    secondary = AccentDark1,
    onSecondary = Color.White,
    background = LightSolidBackgroundBase,
    onBackground = LightTextPrimary,
    surface = LightCardBackground,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardBackgroundSecondary,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightLayerFill,
    outline = LightCardStroke,
    outlineVariant = LightDividerStroke,
    error = SystemCritical,
    onError = Color.White,
)

val FluentDarkColorScheme = darkColorScheme(
    primary = AccentLight2,
    onPrimary = Color(0xFF00243D),
    primaryContainer = AccentLight2.copy(alpha = 0.12f),
    onPrimaryContainer = AccentLight2,
    secondary = AccentDefault,
    onSecondary = Color.White,
    background = DarkSolidBackgroundBase,
    onBackground = DarkTextPrimary,
    surface = DarkCardBackground,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardBackgroundSecondary,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainer = DarkLayerFill,
    outline = DarkCardStroke,
    outlineVariant = DarkDividerStroke,
    error = SystemCriticalDark,
    onError = Color(0xFF3B0A06),
)

// Card hover, one step lighter than the card fill.
val LightCardBackgroundHover = Color(0xFFF9F9F9)
val DarkCardBackgroundHover = Color(0xFF323232)
