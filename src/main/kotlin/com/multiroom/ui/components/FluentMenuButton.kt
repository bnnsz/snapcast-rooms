package com.multiroom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.multiroom.ui.theme.ControlCornerRadius
import com.multiroom.ui.theme.LocalFluent

/** One choice in a [FluentMenuButton]. */
data class MenuAction(
    val label: String,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    val onSelect: () -> Unit,
)

/**
 * Button that opens a menu of related actions.
 *
 * Used where several ways of doing one thing would otherwise need a control
 * each, which reads as separate features rather than alternatives.
 */
@Composable
fun FluentMenuButton(
    text: String,
    actions: List<MenuAction>,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Boolean = false,
    enabled: Boolean = true,
) {
    val fluent = LocalFluent.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var expanded by remember { mutableStateOf(false) }

    val background = when {
        !enabled -> fluent.controlStroke.copy(alpha = 0.35f)
        accent -> if (hovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        else MaterialTheme.colorScheme.primary
        hovered -> fluent.cardBackgroundHover
        else -> fluent.controlFill
    }
    val content = when {
        !enabled -> fluent.textDisabled
        accent -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(modifier) {
        Row(
            modifier = Modifier
                .defaultMinSize(minWidth = 96.dp)
                .height(32.dp)
                .clip(ControlCornerRadius)
                .background(background)
                .border(
                    1.dp,
                    if (accent) Color.Transparent else fluent.controlStroke,
                    ControlCornerRadius,
                )
                .hoverable(interaction)
                .clickable(enabled = enabled) { expanded = true }
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = content)
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Text(action.label, style = MaterialTheme.typography.bodyMedium)
                    },
                    leadingIcon = action.icon?.let {
                        { Icon(it, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    },
                    enabled = action.enabled,
                    onClick = {
                        expanded = false
                        action.onSelect()
                    },
                )
            }
        }
    }
}
