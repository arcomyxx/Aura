package com.dubois.aura

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.dubois.aura.util.LocalInviteToBattle
import com.dubois.aura.util.LocalShareBattle
import com.dubois.aura.util.LocalShareGroupInvite
import com.dubois.aura.util.LocalShareProfile
import com.dubois.aura.util.inviteToBattle
import com.dubois.aura.util.shareBattleCard
import com.dubois.aura.util.shareGroupInvite
import com.dubois.aura.util.shareProfileCard
import com.google.firebase.messaging.FirebaseMessaging
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        configureRevenueCat()

        val pendingToken = intent?.data?.takeIf { it.scheme == "aura" && it.host == "join" }
            ?.lastPathSegment

        setContent {
            CompositionLocalProvider(
                LocalShareProfile provides { name, tierName, score, rank, tierHex ->
                    shareProfileCard(this, name, tierName, score, rank, tierHex)
                },
                LocalShareBattle provides { hostName, battleScore, supporters, bestCombo, auraAwarded, accentHex ->
                    shareBattleCard(this, hostName, battleScore, supporters, bestCombo, auraAwarded, accentHex)
                },
                LocalInviteToBattle provides { hostName ->
                    inviteToBattle(this, hostName)
                },
                LocalShareGroupInvite provides { groupName, token ->
                    shareGroupInvite(this, groupName, token)
                },
            ) {
                App(pendingGroupToken = pendingToken)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerFcmToken()
    }

    private fun registerFcmToken() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            lifecycleScope.launch {
                runCatching {
                    Firebase.firestore.collection("users").document(uid)
                        .update(mapOf("fcmToken" to token))
                }
            }
        }
    }

    private fun configureRevenueCat() {
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

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            AuraMessagingService.CHANNEL_ID,
            AuraMessagingService.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Événements d'aura, battles, invitations."
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
