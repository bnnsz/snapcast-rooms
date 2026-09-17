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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.multiroom.ui.theme.ControlCornerRadius
import com.multiroom.ui.theme.LocalFluent

/** One option in a [FluentComboBox]. */
data class ComboOption<T>(val value: T, val label: String, val icon: ImageVector? = null)

/**
 * Fluent ComboBox: the closed control shows the current selection with a
 * trailing chevron, matching the pickers on Windows 11 Settings rows.
 */
@Composable
fun <T> FluentComboBox(
    selected: T,
    options: List<ComboOption<T>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fluent = LocalFluent.current
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val current = options.firstOrNull { it.value == selected }

    Box(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(ControlCornerRadius)
                .background(
                    when {
                        !enabled -> fluent.controlStroke.copy(alpha = 0.25f)
                        hovered -> fluent.cardBackgroundHover
                        else -> fluent.controlFill
                    }
                )
                .border(1.dp, fluent.controlStroke, ControlCornerRadius)
                .hoverable(interaction)
                .clickable(enabled = enabled) { expanded = true }
                .padding(start = 11.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            current?.icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = current?.label.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else fluent.textDisabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = fluent.textSecondary,
                modifier = Modifier.size(14.dp),
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option.label, style = MaterialTheme.typography.bodyMedium)
                    },
                    leadingIcon = option.icon?.let {
                        {
                            Icon(it, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    },
                    onClick = {
                        onSelect(option.value)
                        expanded = false
                    },
                )
            }
        }
    }
}
