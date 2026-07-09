package com.dubois.aura.model

/**
 * Paliers de rang « aura ». Un utilisateur appartient au tier le plus élevé
 * dont le seuil [minScore] est atteint par son score.
 *
 * Modèle pur (sans dépendance UI) : la couleur associée à chaque tier est
 * définie côté thème (voir `AuraTier.color`).
 */
enum class AuraTier(val displayName: String, val minScore: Int) {
    AURA_NAISSANTE("Aura Naissante", 0),
    ETINCELLE("Étincelle", 2_000),
    LUEUR_MONTANTE("Lueur Montante", 4_000),
    ECLAT_VIF("Éclat Vif", 6_000),
    LUMIERE_DOREE("Lumière Dorée", 8_000),
    AURA_SUPREME("Aura Suprême", 10_000);

    companion object {
        /** Tier correspondant à un score donné. */
        fun forScore(score: Int): AuraTier =
            entries.lastOrNull { score >= it.minScore } ?: AURA_NAISSANTE
    }
}
