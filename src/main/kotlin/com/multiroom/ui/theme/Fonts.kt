package com.multiroom.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import java.io.File

/**
 * The Windows UI typeface, loaded from the installed font files.
 *
 * Each weight is loaded from its own file; resolving by family name yields one
 * face and lets Skia synthesise the rest.
 */
private val FontDir = File(System.getenv("WINDIR") ?: """C:\Windows""", "Fonts")

private fun face(fileName: String, weight: FontWeight) =
    File(FontDir, fileName).takeIf { it.isFile }?.let { Font(it, weight) }

/** Segoe UI at its real weights, falling back to the generic sans. */
val SegoeUI: FontFamily = listOfNotNull(
    face("segoeuil.ttf", FontWeight.Light),
    face("segoeui.ttf", FontWeight.Normal),
    face("seguisb.ttf", FontWeight.SemiBold),
    face("segoeuib.ttf", FontWeight.Bold),
).let { faces -> if (faces.isEmpty()) FontFamily.SansSerif else FontFamily(faces) }

/** Figures that must not reflow. */
val SegoeMono: FontFamily = listOfNotNull(
    face("consola.ttf", FontWeight.Normal),
    face("consolab.ttf", FontWeight.Bold),
).let { faces -> if (faces.isEmpty()) FontFamily.Monospace else FontFamily(faces) }
