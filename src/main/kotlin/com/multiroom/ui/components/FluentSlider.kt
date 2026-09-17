package com.multiroom.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.multiroom.ui.theme.LocalFluent
import kotlinx.coroutines.delay
import kotlin.math.abs

/** How close the echoed value must be before the local position is released. */
private const val EchoTolerance = 0.015f

/** How long to hold the local position when no echo arrives. */
private const val EchoTimeoutMs = 1_200L

// Windows volume-mixer geometry. The row is taller than the thumb to stay
// easy to grab.
private val RailHeight = 3.dp
private val ThumbSize = 12.dp
private val TouchHeight = 20.dp

/**
 * Fluent Slider.
 *
 * The thumb follows the pointer, and holds its local position after release
 * until [value] echoes the change back or the wait times out.
 *
 * @param value current position, 0f..1f
 * @param onValueChange called continuously while dragging, with 0f..1f
 */
@Composable
fun FluentSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val fluent = LocalFluent.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val density = LocalDensity.current

    var widthPx by remember { mutableStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(value) }
    var awaitingEcho by remember { mutableStateOf(false) }

    // Give up if the value never comes back.
    LaunchedEffect(value, awaitingEcho) {
        if (!awaitingEcho) return@LaunchedEffect
        if (abs(value - draft) < EchoTolerance) {
            awaitingEcho = false
        } else {
            delay(EchoTimeoutMs)
            awaitingEcho = false
        }
    }

    val shown = (if (dragging || awaitingEcho) draft else value).coerceIn(0f, 1f)

    // Windows grows the thumb on hover and shrinks it while pressed.
    val thumb by animateDpAsState(
        targetValue = when {
            !enabled -> ThumbSize
            dragging -> ThumbSize - 2.dp
            hovered -> ThumbSize + 2.dp
            else -> ThumbSize
        },
        animationSpec = tween(120),
        label = "thumb",
    )

    val accent = if (enabled) MaterialTheme.colorScheme.primary
    else fluent.controlStroke

    fun report(xPx: Float) {
        if (widthPx <= 0) return
        val thumbPx = with(density) { ThumbSize.toPx() }
        val travel = (widthPx - thumbPx).coerceAtLeast(1f)
        draft = ((xPx - thumbPx / 2f) / travel).coerceIn(0f, 1f)
        onValueChange(draft)
    }

    Box(
        modifier = modifier
            .height(TouchHeight)
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width }
            .hoverable(interaction, enabled = enabled)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onTap = { offset ->
                    report(offset.x)
                    awaitingEcho = true
                })
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        report(offset.x)
                    },
                    onDragEnd = {
                        dragging = false
                        awaitingEcho = true
                    },
                    onDragCancel = {
                        dragging = false
                        awaitingEcho = true
                    },
                    onHorizontalDrag = { change, _ ->
                        report(change.position.x)
                        change.consume()
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumbOffset = with(density) {
            ((widthPx - ThumbSize.toPx()).coerceAtLeast(0f) * shown).toDp()
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(RailHeight)
                .clip(CircleShape)
                .background(fluent.controlStroke),
        )

        Box(
            Modifier
                .fillMaxWidth(shown)
                .height(RailHeight)
                .clip(CircleShape)
                .background(accent),
        )

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumb)
                .clip(CircleShape)
                .background(accent),
        )
    }
}
