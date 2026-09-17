package com.multiroom.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Windows 11 type ramp, in the sizes and weights Settings renders.
// Body and card headers are Regular; only group headers and page titles are
// Semibold. Nothing here is Bold.

val FluentTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = SegoeUI, fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp, lineHeight = 52.sp,
    ),
    // Page title, e.g. "Network & internet"
    headlineMedium = TextStyle(
        fontFamily = SegoeUI, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = SegoeUI, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 28.sp,
    ),
    // Settings card header
    titleMedium = TextStyle(
        fontFamily = SegoeUI, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    // Group header above a set of cards
    titleSmall = TextStyle(
        fontFamily = SegoeUI, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SegoeUI, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = SegoeUI, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    // Caption - card descriptions
    bodySmall = TextStyle(
        fontFamily = SegoeUI, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = SegoeUI, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = SegoeUI, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
)

// Numeric styles
// Tabular figures ("tnum") give every digit the same advance width, so a value
// does not shift as it changes; lining figures ("lnum") keep a common baseline.

val NumericFieldTextStyle = TextStyle(
    fontFamily = SegoeUI, fontWeight = FontWeight.Normal,
    fontSize = 14.sp, lineHeight = 20.sp,
    fontFeatureSettings = "tnum, lnum",
)

val NumericValueTextStyle = TextStyle(
    fontFamily = SegoeUI, fontWeight = FontWeight.Normal,
    fontSize = 14.sp, lineHeight = 20.sp,
    fontFeatureSettings = "tnum, lnum",
)

val MetricValueTextStyle = TextStyle(
    fontFamily = SegoeUI, fontWeight = FontWeight.Normal,
    fontSize = 14.sp, lineHeight = 20.sp,
    fontFeatureSettings = "tnum, lnum",
)

val UnitTextStyle = TextStyle(
    fontFamily = SegoeUI, fontWeight = FontWeight.Normal,
    fontSize = 12.sp, lineHeight = 16.sp,
    fontFeatureSettings = "tnum, lnum",
)
