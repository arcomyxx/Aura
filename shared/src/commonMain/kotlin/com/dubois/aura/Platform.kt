package com.dubois.aura

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform