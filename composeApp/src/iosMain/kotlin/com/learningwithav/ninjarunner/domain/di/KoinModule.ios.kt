package com.learningwithav.ninjarunner.domain.di

import com.learningwithav.ninjarunner.domain.audio.AudioPlayer
import org.koin.dsl.module

actual val targetModule = module {
    single<AudioPlayer> { AudioPlayer() }
}