package com.learningwithav.ninjarunner

import android.app.Application
import com.learningwithav.ninjarunner.domain.di.initializeKoin
import org.koin.android.ext.koin.androidContext

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        initializeKoin {
            androidContext(this@MyApplication)
        }
    }
}