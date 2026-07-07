package com.dubois.aura.model

import kotlinx.serialization.Serializable

/**
 * Ami confirmé, stocké dans la sous-collection `users/{uid}/friends/{friendUid}`.
 * C'est un instantané léger du profil de l'ami au moment de l'acceptation.
 */
@Serializable
data class Friend(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val since: Long = 0L,
)
