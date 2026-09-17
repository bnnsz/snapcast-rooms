package com.multiroom.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.multiroom.model.ServerGroup
import com.multiroom.ui.components.CardGroupHeader
import com.multiroom.ui.components.ComboOption
import com.multiroom.ui.components.FluentComboBox
import com.multiroom.ui.components.FluentToggle
import com.multiroom.ui.components.InfoBar
import com.multiroom.ui.components.SettingsCard
import com.multiroom.ui.theme.CardGap
import com.multiroom.ui.theme.CardGroupGap
import com.multiroom.ui.theme.LocalFluent
import com.multiroom.ui.theme.NumericValueTextStyle
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp

/**
 * Snapserver groups. Players in one group play the same stream in sync; players in
 * different groups do not, whatever is playing.
 *
 * A player is moved by choosing its group, because Snapserver has no concept of
 * removing a client from a group: it only ever assigns one to a group, and a
 * client left without one is placed in a newly created group.
 */
@Composable
fun GroupsScreen(
    groups: List<ServerGroup>,
    onMoveClientToGroup: (clientId: String, groupId: String) -> Unit,
    onSetGroupMute: (String, Boolean) -> Unit,
) {
    if (groups.isEmpty()) {
        InfoBar(
            title = "No groups",
            message = "Groups appear once a player connects to the server.",
        )
        return
    }

    val fluent = LocalFluent.current
    val groupOptions = groups.mapIndexed { index, group ->
        ComboOption(group.id, "Group ${index + 1}", Icons.Outlined.Group)
    }

    Column(verticalArrangement = Arrangement.spacedBy(CardGroupGap)) {
        groups.forEachIndexed { index, group ->
            Column {
                CardGroupHeader("Group ${index + 1}")

                Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
                    SettingsCard(
                        header = "Mute this group",
                        description = "Playing ${group.streamId.ifEmpty { "nothing" }}",
                        icon = if (group.muted) Icons.Outlined.VolumeOff
                        else Icons.Outlined.VolumeUp,
                    ) {
                        FluentToggle(
                            checked = group.muted,
                            onCheckedChange = { onSetGroupMute(group.id, it) },
                        )
                    }

                    if (group.clients.isEmpty()) {
                        SettingsCard(
                            header = "No players in this group",
                            description = "Move a player here from another group",
                            icon = Icons.Outlined.Speaker,
                        )
                    }

                    group.clients.forEach { client ->
                        SettingsCard(
                            header = client.displayName,
                            description = if (client.connected) "Connected"
                            else "Not connected",
                            icon = Icons.Outlined.Speaker,
                        ) {
                            Text(
                                text = "${client.volumePercent}%",
                                style = NumericValueTextStyle,
                                color = fluent.textSecondary,
                            )
                            FluentComboBox(
                                selected = group.id,
                                options = groupOptions,
                                onSelect = { target ->
                                    if (target != group.id) {
                                        onMoveClientToGroup(client.id, target)
                                    }
                                },
                                modifier = Modifier.width(150.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
