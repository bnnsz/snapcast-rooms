package com.multiroom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.multiroom.ui.theme.ControlCornerRadius
import com.multiroom.ui.theme.LocalFluent

/**
 * Fluent button.
 *
 * @param accent render as the accent style, for the primary action of a dialog
 */
@Composable
fun FluentButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Boolean = false,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val fluent = LocalFluent.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    // Windows draws a destructive action as red text, not a red fill.
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
        destructive -> fluent.critical
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 88.dp, minHeight = 32.dp)
            .clip(ControlCornerRadius)
            .background(background)
            .border(1.dp, if (accent) Color.Transparent else fluent.controlStroke, ControlCornerRadius)
            .hoverable(interaction)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = content)
    }
}

/** Fluent ToggleSwitch: a pill track with a knob that grows on hover. */
@Composable
fun FluentToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fluent = LocalFluent.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val track = when {
        !enabled -> fluent.controlStroke.copy(alpha = 0.4f)
        checked -> MaterialTheme.colorScheme.primary
        hovered -> fluent.cardBackgroundHover
        else -> fluent.controlFill
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // Windows labels the state beside the switch.
        Text(
            text = if (checked) "On" else "Off",
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else fluent.textDisabled,
            modifier = Modifier.width(26.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(50))
                .background(track)
                .border(
                    width = 1.dp,
                    color = if (checked) Color.Transparent
                    else fluent.textSecondary.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(50),
                )
                .hoverable(interaction)
                .clickable(enabled = enabled) { onCheckedChange(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (hovered) 14.dp else 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (checked) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    ),
            )
        }
    }
}

/**
 * Fluent InfoBar: a full-width notice used for empty states and warnings.
 *
 * @param severity tints the strip; null renders the informational style
 */
@Composable
fun InfoBar(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    severity: Color? = null,
) {
    val fluent = LocalFluent.current
    val tint = severity ?: MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ControlCornerRadius)
            .background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.25f), ControlCornerRadius)
            .padding(16.dp),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(50))
                .background(tint),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = fluent.textSecondary,
            )
        }
    }
}
