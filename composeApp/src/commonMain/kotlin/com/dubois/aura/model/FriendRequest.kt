package com.dubois.aura.model

import kotlinx.serialization.Serializable

/**
 * Demande d'ami stockée dans la collection Firestore `friendRequests`.
 * `status` : "pending" | "accepted" | "declined".
 */
@Serializable
data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromEmail: String = "",
    val toUid: String = "",
    val toName: String = "",
    val toEmail: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L,
)
