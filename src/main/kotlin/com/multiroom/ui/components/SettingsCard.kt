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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
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
import com.multiroom.ui.theme.CardCornerRadius
import com.multiroom.ui.theme.CardIconSize
import com.multiroom.ui.theme.LocalFluent
import com.multiroom.ui.theme.SettingsCardHeight
import com.multiroom.ui.theme.SettingsCardPadding

/**
 * A settings row: a leading icon, a header with an optional description, and
 * trailing content.
 *
 * @param chevron show the navigation chevron used on rows that open a subpage
 * @param onClick makes the whole row actionable
 */
@Composable
fun SettingsCard(
    header: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val fluent = LocalFluent.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = SettingsCardHeight)
            .clip(CardCornerRadius)
            .background(
                if (hovered && onClick != null) fluent.cardBackgroundHover
                else MaterialTheme.colorScheme.surface
            )
            .border(1.dp, fluent.cardStroke, CardCornerRadius)
            .then(
                if (onClick != null) Modifier.hoverable(interaction).clickable(onClick = onClick)
                else Modifier
            )
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
                    maxLines = 2,
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

        if (chevron) {
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = fluent.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Header above a set of cards, as Settings labels its sections. */
@Composable
fun CardGroupHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(bottom = 8.dp),
    )
}
