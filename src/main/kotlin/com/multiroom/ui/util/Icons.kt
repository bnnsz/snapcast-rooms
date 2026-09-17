package com.multiroom.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.unit.Density

/**
 * Loads an SVG from the classpath as a [Painter].
 *
 * @param path resource path, e.g. "snapcast-logo.svg"
 */
@Composable
fun rememberSvgPainter(path: String): Painter {
    val density = LocalDensity.current
    return remember(path, density) { loadSvg(path, density) }
}

/** Non-composable variant, for window and tray icons created outside composition. */
fun loadSvg(path: String, density: Density): Painter =
    Thread.currentThread().contextClassLoader.getResourceAsStream(path).use { stream ->
        requireNotNull(stream) { "resource not found: $path" }
        loadSvgPainter(stream, density)
    }
