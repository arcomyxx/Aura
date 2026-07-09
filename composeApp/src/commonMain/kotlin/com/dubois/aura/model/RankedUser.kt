package com.dubois.aura.model

/**
 * Utilisateur enrichi de sa position au classement global. Modèle d'affichage
 * calculé côté ViewModel, utilisé par les onglets Amis et Classement.
 */
data class RankedUser(
    val profile: UserProfile,
    val rank: Int,
    val isMe: Boolean,
) {
    val tier: AuraTier get() = AuraTier.forScore(profile.auraScore)
}
