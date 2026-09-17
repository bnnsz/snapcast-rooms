package com.multiroom.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.multiroom.ui.theme.ControlCornerRadius
import com.multiroom.ui.theme.LocalFluent
import com.multiroom.ui.theme.NumericFieldTextStyle
import com.multiroom.ui.theme.UnitTextStyle

/** What a field accepts; decides filtering, alignment and typography. */
enum class FieldKind { TEXT, NUMBER, IP_ADDRESS }

/** Windows TextBox height, shared with ComboBox and Button. */
private val TextBoxHeight = 32.dp

/**
 * Fluent TextBox. Carries no label of its own; the settings row supplies it.
 *
 * Numeric kinds are centred, use tabular figures, and reject characters that
 * cannot occur in the value. Edits report on Enter or focus loss, and Escape
 * restores the last committed value.
 *
 * @param suffix unit rendered inside the trailing edge, e.g. "ms"
 * @param commitWhileTyping report every keystroke instead
 */
@Composable
fun FluentTextBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    kind: FieldKind = FieldKind.TEXT,
    suffix: String? = null,
    enabled: Boolean = true,
    commitWhileTyping: Boolean = false,
) {
    val fluent = LocalFluent.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val focusManager = LocalFocusManager.current

    var draft by remember { mutableStateOf(value) }
    var editing by remember { mutableStateOf(false) }

    LaunchedEffect(value) { if (!editing) draft = value }

    fun commit() {
        if (draft != value) onValueChange(draft)
    }

    val accentHeight by animateDpAsState(
        targetValue = if (focused) 2.dp else 1.dp,
        animationSpec = tween(120),
        label = "accentHeight",
    )
    val accentColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary
        else fluent.textSecondary.copy(alpha = 0.55f),
        animationSpec = tween(120),
        label = "accentColor",
    )

    val numeric = kind != FieldKind.TEXT
    val style = (if (numeric) NumericFieldTextStyle else MaterialTheme.typography.bodyMedium)
        .copy(
            color = if (enabled) MaterialTheme.colorScheme.onSurface else fluent.textDisabled,
            textAlign = if (numeric) TextAlign.Center else TextAlign.Start,
        )

    Box(
        modifier = modifier
            .height(TextBoxHeight)
            .clip(ControlCornerRadius)
            .background(
                when {
                    !enabled -> fluent.controlStroke.copy(alpha = 0.25f)
                    focused -> MaterialTheme.colorScheme.surface
                    hovered -> fluent.cardBackgroundHover
                    else -> fluent.controlFill
                }
            )
            .border(1.dp, fluent.controlStroke, ControlCornerRadius)
            .hoverable(interaction),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = draft,
                onValueChange = {
                    draft = filter(it, kind)
                    if (commitWhileTyping) onValueChange(draft)
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            editing = true
                        } else if (editing) {
                            editing = false
                            commit()
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Enter, Key.NumPadEnter -> {
                                commit()
                                focusManager.clearFocus()
                                true
                            }
                            Key.Escape -> {
                                draft = value
                                focusManager.clearFocus()
                                true
                            }
                            else -> false
                        }
                    },
                enabled = enabled,
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text
                ),
                interactionSource = interaction,
            )

            if (suffix != null) {
                Spacer(Modifier.width(6.dp))
                Text(text = suffix, style = UnitTextStyle, color = fluent.textSecondary)
            }
        }

        // Inside the rounded clip, so it stops at the corners.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(accentHeight)
                .background(accentColor),
        )
    }
}

/** Drops characters that cannot appear in a value of this [kind]. */
private fun filter(input: String, kind: FieldKind): String = when (kind) {
    FieldKind.TEXT -> input
    FieldKind.NUMBER -> input.filter(Char::isDigit).take(5)
    FieldKind.IP_ADDRESS -> input.filter { it.isDigit() || it == '.' }.take(15)
}
