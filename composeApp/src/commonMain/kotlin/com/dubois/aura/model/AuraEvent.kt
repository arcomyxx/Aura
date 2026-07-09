package com.dubois.aura.model

import kotlinx.serialization.Serializable

/**
 * Événement d'aura signalé sur un utilisateur, stocké dans la sous-collection
 * `users/{targetUid}/events/{eventId}`. Chaque événement modifie le score
 * d'aura de la cible de [delta] points.
 */
@Serializable
data class AuraEvent(
    val id: String = "",
    val targetUid: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val typeId: String = "",
    val emoji: String = "",
    val label: String = "",
    val delta: Int = 0,
    val description: String = "",
    val createdAt: Long = 0L,
)
