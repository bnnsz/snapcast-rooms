package com.multiroom.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Fluent colours that have no Material3 equivalent. */
data class FluentPalette(
    val cardStroke: Color,
    val iconTint: Color,
    val cardBackgroundHover: Color,
    val dividerStroke: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val subtleHover: Color,
    val subtlePressed: Color,
    val controlFill: Color,
    val controlStroke: Color,
    val success: Color,
    val caution: Color,
    val critical: Color,
)

val LocalFluent = staticCompositionLocalOf {
    FluentPalette(
        cardStroke = LightCardStroke,
        iconTint = LightIconTint,
        cardBackgroundHover = LightCardBackgroundHover,
        dividerStroke = LightDividerStroke,
        textSecondary = LightTextSecondary,
        textDisabled = LightTextDisabled,
        subtleHover = LightSubtleFillSecondary,
        subtlePressed = LightSubtleFillTertiary,
        controlFill = LightControlFill,
        controlStroke = LightControlStroke,
        success = SystemSuccess,
        caution = SystemCaution,
        critical = SystemCritical,
    )
}

private val FluentShapes = Shapes(
    extraSmall = ControlCornerRadius,
    small = ControlCornerRadius,
    medium = OverlayCornerRadius,
    large = OverlayCornerRadius,
    extraLarge = OverlayCornerRadius,
)

/**
 * Applies the Fluent theme.
 *
 * @param dark render the dark scheme; follows the operating system by default
 */
@Composable
fun RoomsTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (dark) {
        FluentPalette(
            cardStroke = DarkCardStroke,
            iconTint = DarkIconTint,
            cardBackgroundHover = DarkCardBackgroundHover,
            dividerStroke = DarkDividerStroke,
            textSecondary = DarkTextSecondary,
            textDisabled = DarkTextDisabled,
            subtleHover = DarkSubtleFillSecondary,
            subtlePressed = DarkSubtleFillTertiary,
            controlFill = DarkControlFill,
            controlStroke = DarkControlStroke,
            success = SystemSuccessDark,
            caution = SystemCautionDark,
            critical = SystemCriticalDark,
        )
    } else {
        FluentPalette(
            cardStroke = LightCardStroke,
            iconTint = LightIconTint,
            cardBackgroundHover = LightCardBackgroundHover,
            dividerStroke = LightDividerStroke,
            textSecondary = LightTextSecondary,
            textDisabled = LightTextDisabled,
            subtleHover = LightSubtleFillSecondary,
            subtlePressed = LightSubtleFillTertiary,
            controlFill = LightControlFill,
            controlStroke = LightControlStroke,
            success = SystemSuccess,
            caution = SystemCaution,
            critical = SystemCritical,
        )
    }

    CompositionLocalProvider(LocalFluent provides palette) {
        MaterialTheme(
            colorScheme = if (dark) FluentDarkColorScheme else FluentLightColorScheme,
            typography = FluentTypography,
            shapes = FluentShapes,
            content = content,
        )
    }
}
