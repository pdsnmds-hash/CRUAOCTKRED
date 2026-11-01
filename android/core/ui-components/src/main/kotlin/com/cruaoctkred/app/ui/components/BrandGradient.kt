package com.cruaoctkred.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.lerp

/** Shared brand fill; pale panels retain their contrast through strength. */
@Composable
fun Modifier.brandGradient(shape: Shape = RectangleShape, strength: Float = 1f): Modifier =
    background(Brush.linearGradient(listOf(
        lerp(Color.White, MaterialTheme.colorScheme.secondary, strength),
        lerp(Color.White, MaterialTheme.colorScheme.primary, strength),
    )), shape)
