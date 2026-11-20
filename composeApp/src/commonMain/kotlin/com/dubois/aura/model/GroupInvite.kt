package com.dubois.aura.model

import kotlinx.serialization.Serializable

/**
 * Invitation à rejoindre un groupe. Stockée dans `groupInvites/{inviteId}`.
 * status : "pending" | "accepted" | "declined"
 */
@Serializable
data class GroupInvite(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val toUid: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L,
)
