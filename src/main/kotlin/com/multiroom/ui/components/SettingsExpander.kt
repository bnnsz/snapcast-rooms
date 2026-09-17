package com.multiroom.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.multiroom.ui.theme.CardCornerRadius
import com.multiroom.ui.theme.FluentMotion
import com.multiroom.ui.theme.CardIconSize
import com.multiroom.ui.theme.LocalFluent
import com.multiroom.ui.theme.SettingsCardHeight
import com.multiroom.ui.theme.SettingsCardPadding

/**
 * Expander card: a [SettingsCard] header with a chevron, over a region that
 * reveals further rows.
 *
 * @param trailing controls shown in the collapsed header, left of the chevron
 * @param content revealed when expanded
 */
@Composable
fun SettingsExpander(
    header: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    initiallyExpanded: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val fluent = LocalFluent.current
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = FluentMotion.standard(FluentMotion.FAST),
        label = "chevron",
    )

    val headerShape = if (expanded) {
        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
    } else {
        CardCornerRadius
    }
    val bodyShape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = SettingsCardHeight)
                .clip(headerShape)
                .background(
                    if (hovered) fluent.cardBackgroundHover
                    else MaterialTheme.colorScheme.surface
                )
                .border(1.dp, fluent.cardStroke, headerShape)
                .hoverable(interaction)
                .clickable { expanded = !expanded }
                .padding(horizontal = SettingsCardPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: fluent.iconTint,
                    modifier = Modifier.size(CardIconSize),
                )
                Spacer(Modifier.width(SettingsCardPadding))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = fluent.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                content = trailing,
            )

            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = fluent.textSecondary,
                modifier = Modifier.size(16.dp).rotate(chevronRotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(FluentMotion.enter()) + fadeIn(FluentMotion.enter()),
            exit = shrinkVertically(FluentMotion.exit()) + fadeOut(FluentMotion.exit()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(bodyShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, fluent.cardStroke, bodyShape),
                content = content,
            )
        }
    }
}

/**
 * A row inside an expanded [SettingsExpander], indented to the header text.
 */
@Composable
fun ExpanderRow(
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    showDivider: Boolean = true,
    trailing: @Composable RowScope.() -> Unit,
) {
    val fluent = LocalFluent.current
    Column(modifier.fillMaxWidth()) {
        if (showDivider) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(fluent.dividerStroke)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 60.dp)
                .padding(start = 52.dp, end = SettingsCardPadding, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = fluent.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                content = trailing,
            )
        }
    }
}
