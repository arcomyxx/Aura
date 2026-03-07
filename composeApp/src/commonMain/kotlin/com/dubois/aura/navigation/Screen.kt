package com.dubois.aura.navigation

sealed class Screen(val route: String) {
    object Home     : Screen("home")
    object Login    : Screen("login")
    object Register : Screen("register")
    object Profile  : Screen("profile")
}