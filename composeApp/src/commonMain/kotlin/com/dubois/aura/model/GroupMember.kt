package com.dubois.aura.model

import kotlinx.serialization.Serializable

/**
 * Membre d'un groupe avec son score d'aura propre au groupe.
 * Stocké dans `groups/{groupId}/members/{uid}`.
 * Ce score est indépendant du score global de l'utilisateur.
 */
@Serializable
data class GroupMember(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val groupAuraScore: Int = 0,
) {
    val tier: AuraTier get() = AuraTier.forScore(groupAuraScore)
}
