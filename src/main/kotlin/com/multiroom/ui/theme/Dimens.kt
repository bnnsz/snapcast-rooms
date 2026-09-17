package com.multiroom.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Measured against Windows 11 Settings.

val ControlCornerRadius = RoundedCornerShape(4.dp)
val CardCornerRadius = RoundedCornerShape(4.dp)
val OverlayCornerRadius = RoundedCornerShape(8.dp)
val PillCornerRadius = RoundedCornerShape(50)

// Navigation pane

val NavPaneWidth = 290.dp
val NavPaneCompactWidth = 48.dp

// WinUI NavigationView breakpoints: below the first the pane collapses to
// icons, below the second it is hidden behind a menu button.
val NavExpandedThreshold = 1008.dp
val NavCompactThreshold = 641.dp
val NavItemHeight = 40.dp
val NavIconSize = 20.dp
val NavIndicatorWidth = 3.dp
val NavIndicatorHeight = 16.dp
val AvatarSize = 48.dp

// Content

/** Settings left-aligns its content and stops it growing past this. */
val ContentMaxWidth = 1000.dp
val ContentStartPadding = 28.dp
val ContentTopPadding = 20.dp

// Cards
// Settings rows are separate cards with a gap between them, not a joined group.

/** WinUI SettingsCard minimum height. */
val SettingsCardHeight = 68.dp
val SettingsCardPadding = 16.dp
val CardGap = 6.dp
val CardGroupGap = 24.dp
val CardIconSize = 20.dp

val TitleBarHeight = 40.dp
