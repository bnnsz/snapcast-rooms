package com.multiroom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.multiroom.ui.theme.ControlCornerRadius
import com.multiroom.ui.theme.LocalFluent

/** Windows subtle command size. */
private val ButtonSize = 32.dp

/**
 * Fluent subtle icon button: a glyph with no chrome until it is pointed at.
 *
 * Windows draws commands inside a list row without a border or fill, so a
 * bordered control reads as a second card rather than an action.
 *
 * @param accent tints the glyph with the accent colour, for a primary action
 * @param description what the button does, for accessibility
 */
@Composable
fun FluentIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    enabled: Boolean = true,
) {
    val fluent = LocalFluent.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val background = when {
        !enabled -> Color.Transparent
        hovered -> fluent.subtleHover
        else -> Color.Transparent
    }
    val tint = when {
        !enabled -> fluent.textDisabled
        accent -> MaterialTheme.colorScheme.primary
        else -> fluent.iconTint
    }

    Box(
        modifier = modifier
            .size(ButtonSize)
            .clip(ControlCornerRadius)
            .background(background)
            .hoverable(interaction, enabled = enabled)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}
