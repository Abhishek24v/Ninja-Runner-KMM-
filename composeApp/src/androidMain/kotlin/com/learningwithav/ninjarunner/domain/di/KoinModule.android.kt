package com.learningwithav.ninjarunner.domain.di

import com.learningwithav.ninjarunner.domain.audio.AudioPlayer
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

@OptIn(ExperimentalResourceApi::class)
actual val targetModule = module {
    single<AudioPlayer> { AudioPlayer(context = androidContext()) }
}