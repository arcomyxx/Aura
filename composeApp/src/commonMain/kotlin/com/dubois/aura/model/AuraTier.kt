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
    ETINCELLE("Étincelle", 200),
    LUEUR_MONTANTE("Lueur Montante", 400),
    ECLAT_VIF("Éclat Vif", 600),
    LUMIERE_DOREE("Lumière Dorée", 800),
    AURA_SUPREME("Aura Suprême", 1_000);

    companion object {
        /** Tier correspondant à un score donné. */
        fun forScore(score: Int): AuraTier =
            entries.lastOrNull { score >= it.minScore } ?: AURA_NAISSANTE
    }
}
