package com.learningwithav.ninjarunner

import androidx.compose.ui.window.ComposeUIViewController
import com.learningwithav.ninjarunner.domain.di.initializeKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        initializeKoin()
    }
) { App() }