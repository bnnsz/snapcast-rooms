package com.multiroom.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * WinUI motion: asymmetric curves, where an element entering decelerates into
 * place and one leaving accelerates away over a shorter duration.
 */
object FluentMotion {

    /** ControlFastOutSlowInKeySpline - entrances and moves. */
    val Decelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)

    /** Exits, which accelerate out of view. */
    val Accelerate: Easing = CubicBezierEasing(0.7f, 0f, 1f, 0.5f)

    /** Standard easing for state changes that stay on screen. */
    val Standard: Easing = CubicBezierEasing(0.33f, 0f, 0.67f, 1f)

    const val FAST = 150
    const val NORMAL = 250
    const val SLOW = 350

    fun <T> enter(durationMs: Int = NORMAL): FiniteAnimationSpec<T> =
        tween(durationMs, easing = Decelerate)

    fun <T> exit(durationMs: Int = FAST): FiniteAnimationSpec<T> =
        tween(durationMs, easing = Accelerate)

    fun <T> standard(durationMs: Int = NORMAL): FiniteAnimationSpec<T> =
        tween(durationMs, easing = Standard)
}
