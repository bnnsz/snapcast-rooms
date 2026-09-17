package com.multiroom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.multiroom.ui.theme.LocalFluent
import com.multiroom.ui.theme.OverlayCornerRadius

/**
 * Fluent ContentDialog: a modal card over a dimmed window.
 *
 * Escape dismisses. The confirming action is the accent button, as in Windows.
 *
 * @param confirmEnabled whether the dialog holds a value worth committing
 */
@Composable
fun FluentDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val fluent = LocalFluent.current

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .onPreviewKeyEvent { event ->
                    val dismiss = event.type == KeyEventType.KeyDown && event.key == Key.Escape
                    if (dismiss) onDismiss()
                    dismiss
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .clip(OverlayCornerRadius)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, fluent.cardStroke, OverlayCornerRadius),
            ) {
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        content = content,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FluentButton(
                        text = confirmText,
                        modifier = Modifier.weight(1f),
                        accent = true,
                        enabled = confirmEnabled,
                        onClick = onConfirm,
                    )
                    FluentButton(
                        text = "Cancel",
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}
