package com.learningwithav.ninjarunner.domain.target

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.Color


data class StrongTarget(
    override val x: Float = 0f,
    override val y: Animatable<Float, *> = Animatable(0f),
    override val radius: Float = 0f,
    override val fallingSpeed: Float = 0f,
    override val color: Color = Color.Red,
    val lives: Int = 3
): Target
