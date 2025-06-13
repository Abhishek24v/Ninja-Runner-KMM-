package com.learningwithav.ninjarunner

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform