package com.multiroom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.multiroom.ui.theme.ControlCornerRadius
import com.multiroom.ui.theme.LocalFluent
import com.multiroom.ui.theme.SystemCritical
import com.multiroom.ui.theme.TitleBarHeight

/**
 * Windows 11 caption bar, supplying dragging and the caption buttons for an
 * undecorated window.
 */
@Composable
fun WindowScope.TitleBar(
    title: String,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit,
    onClose: () -> Unit,
) {
    val fluent = LocalFluent.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TitleBarHeight)
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Spacer(Modifier.width(6.dp))
        if (showMenuButton) {
            CaptionAction(Icons.Outlined.Menu, "Show navigation", onMenuClick)
            Spacer(Modifier.width(2.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = fluent.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = if (showMenuButton) 2.dp else 8.dp),
        )

        WindowDraggableArea(Modifier.weight(1f).fillMaxHeight()) {
            Box(Modifier.fillMaxWidth().fillMaxHeight())
        }

        CaptionButton(
            icon = Icons.Outlined.Close,
            description = "Close to tray",
            hoverBackground = SystemCritical,
            hoverTint = Color.White,
            onClick = onClose,
        )
    }
}

/** Square action in the caption bar, sized to the bar rather than a caption button. */
@Composable
private fun CaptionAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    val fluent = LocalFluent.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(ControlCornerRadius)
            .background(if (hovered) fluent.subtleHover else Color.Transparent)
            .hoverable(interaction)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun CaptionButton(
    icon: ImageVector,
    description: String,
    hoverBackground: Color? = null,
    hoverTint: Color? = null,
    onClick: () -> Unit,
) {
    val fluent = LocalFluent.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(
                when {
                    !hovered -> Color.Transparent
                    hoverBackground != null -> hoverBackground
                    else -> fluent.subtleHover
                }
            )
            .hoverable(interaction)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (hovered && hoverTint != null) hoverTint
            else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(14.dp),
        )
    }
}
