package com.dubois.aura.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val themeColor: String = "",
    val createdAt: Long = 0L,
    val auraScore: Int = 0,
    val isPremium: Boolean = false,
    val premiumExpiresAt: Long? = null,
    val premiumEntitlementId: String = "",
)