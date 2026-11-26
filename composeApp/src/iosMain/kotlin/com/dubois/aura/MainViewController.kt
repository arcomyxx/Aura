package com.dubois.aura

import androidx.compose.ui.window.ComposeUIViewController
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

fun MainViewController() = ComposeUIViewController {
    App()
}

fun configureRevenueCat() {
    Purchases.logLevel = LogLevel.DEBUG
    Purchases.configure(
        PurchasesConfiguration.Builder(
            apiKey = "test_uGOweGBWHBsvJwBAKxnTXHkDuwt",
        ).build()
    )
    Firebase.auth.currentUser?.uid?.let { uid ->
        Purchases.sharedInstance.logIn(uid, onError = { _ -> }) { _, _ -> }
    }
}