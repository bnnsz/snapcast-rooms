package com.multiroom.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.multiroom.ui.theme.AvatarSize
import com.multiroom.ui.theme.ControlCornerRadius
import com.multiroom.ui.theme.FluentMotion
import com.multiroom.ui.theme.LocalFluent
import com.multiroom.ui.theme.NavIconSize
import com.multiroom.ui.theme.NavIndicatorHeight
import com.multiroom.ui.theme.NavIndicatorWidth
import com.multiroom.ui.theme.NavItemHeight
import com.multiroom.ui.theme.NavPaneCompactWidth
import com.multiroom.ui.theme.NavPaneWidth
import com.multiroom.ui.util.rememberSvgPainter

/**
 * Top-level sections, each with its own icon colour.
 */
enum class Screen(val label: String, val icon: ImageVector, val tint: Color) {
    Players("Players", Icons.Outlined.Speaker, Color(0xFF0078D4)),
    Groups("Groups", Icons.Outlined.Groups, Color(0xFF8764B8)),
    Diagnostics("Diagnostics", Icons.Outlined.GraphicEq, Color(0xFF0E8A6A)),
    Settings("Settings", Icons.Outlined.Settings, Color(0xFF6B6B6B)),
}

/** How much of the pane is shown, following the WinUI display modes. */
enum class NavMode { Expanded, Compact, Minimal }

/**
 * NavigationView pane. Selection is a subtle fill with an accent pill at the
 * leading edge; [NavMode.Compact] draws the icons alone.
 */
@Composable
fun NavigationPane(
    current: Screen,
    onNavigate: (Screen) -> Unit,
    serverLabel: String,
    mode: NavMode,
    modifier: Modifier = Modifier,
) {
    val fluent = LocalFluent.current
    val compact = mode == NavMode.Compact
    val paneWidth by animateDpAsState(
        targetValue = if (compact) NavPaneCompactWidth else NavPaneWidth,
        animationSpec = FluentMotion.standard(),
        label = "paneWidth",
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(paneWidth)
            .padding(horizontal = if (compact) 4.dp else 12.dp),
        horizontalAlignment = if (compact) Alignment.CenterHorizontally
        else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (compact) {
            Box(
                Modifier.padding(vertical = 14.dp).size(28.dp).clip(CircleShape)
                    .background(fluent.controlFill),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = rememberSvgPainter("snapcast-logo.svg"),
                    contentDescription = "Snapcast Rooms",
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(AvatarSize).clip(CircleShape).background(fluent.controlFill),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = rememberSvgPainter("snapcast-logo.svg"),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Snapcast Rooms",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = serverLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = fluent.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Screen.entries.forEach { screen ->
            NavItem(
                screen = screen,
                selected = current == screen,
                compact = compact,
                onClick = { onNavigate(screen) },
            )
        }
    }
}

@Composable
private fun NavItem(
    screen: Screen,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val fluent = LocalFluent.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val indicatorHeight by animateDpAsState(
        targetValue = if (selected) NavIndicatorHeight else 0.dp,
        animationSpec = FluentMotion.standard(FluentMotion.FAST),
        label = "navIndicator",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(NavItemHeight)
            .clip(ControlCornerRadius)
            .background(
                when {
                    selected -> fluent.subtleHover
                    hovered -> fluent.subtlePressed
                    else -> Color.Transparent
                }
            )
            .hoverable(interaction)
            .clickable(onClick = onClick),
        contentAlignment = if (compact) Alignment.Center else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .width(NavIndicatorWidth)
                .height(indicatorHeight)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
        )

        if (compact) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.label,
                tint = screen.tint,
                modifier = Modifier.size(NavIconSize),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = screen.icon,
                    contentDescription = null,
                    tint = screen.tint,
                    modifier = Modifier.size(NavIconSize),
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = screen.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
