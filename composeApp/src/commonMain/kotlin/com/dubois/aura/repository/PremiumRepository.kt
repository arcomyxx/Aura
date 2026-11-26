package com.dubois.aura.repository

import com.dubois.aura.model.UserProfile
import com.revenuecat.purchases.kmp.CustomerInfo
import com.revenuecat.purchases.kmp.Offerings
import com.revenuecat.purchases.kmp.Package
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val REVENUECAT_ENTITLEMENT = "premium"

sealed class PremiumStatus {
    data class Active(val expiresAt: Long?) : PremiumStatus()
    data object Inactive : PremiumStatus()
    data object Unknown : PremiumStatus()
}

class PremiumRepository {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private fun myUid(): String? = auth.currentUser?.uid

    /** Statut premium depuis Firestore (source de vérité serveur). */
    fun premiumStatus(): Flow<PremiumStatus> {
        val uid = myUid() ?: return kotlinx.coroutines.flow.flowOf(PremiumStatus.Unknown)
        return firestore.collection("users").document(uid).snapshots.map { snap ->
            if (!snap.exists) return@map PremiumStatus.Unknown
            val profile = snap.data(UserProfile.serializer())
            if (profile.isPremium) PremiumStatus.Active(profile.premiumExpiresAt)
            else PremiumStatus.Inactive
        }
    }

    /** Offres RevenueCat disponibles. */
    suspend fun getOfferings(): Result<Offerings> = runCatching {
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.getOfferings(
                onError = { cont.resumeWithException(Exception(it.message)) },
                onSuccess = { cont.resume(it) },
            )
        }
    }

    /** Déclenche un achat. */
    suspend fun purchase(pkg: Package): Result<CustomerInfo> = runCatching {
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.purchase(
                packageToPurchase = pkg,
                onError = { error, _ -> cont.resumeWithException(Exception(error.message)) },
                onSuccess = { _, customerInfo -> cont.resume(customerInfo) },
            )
        }
    }

    /** Restaure les achats existants. */
    suspend fun restorePurchases(): Result<CustomerInfo> = runCatching {
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.restorePurchases(
                onError = { cont.resumeWithException(Exception(it.message)) },
                onSuccess = { cont.resume(it) },
            )
        }
    }

    /** CustomerInfo (snapshot unique, pour confirmation immédiate). */
    fun customerInfoFlow(): Flow<CustomerInfo?> = kotlinx.coroutines.flow.flow {
        emit(runCatching {
            suspendCancellableCoroutine { cont ->
                Purchases.sharedInstance.getCustomerInfo(
                    onError = { cont.resume(null) },
                    onSuccess = { cont.resume(it) },
                )
            }
        }.getOrNull())
    }
}
