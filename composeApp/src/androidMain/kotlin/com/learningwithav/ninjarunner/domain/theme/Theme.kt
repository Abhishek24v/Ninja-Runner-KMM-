package com.learningwithav.ninjarunner.domain.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable


@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {

//    val colors = when {
//        useDarkTheme -> DarkColors
//        else -> LightColors
//    }

    MaterialTheme(
       // colorScheme = colors,
        content = content
    )
}