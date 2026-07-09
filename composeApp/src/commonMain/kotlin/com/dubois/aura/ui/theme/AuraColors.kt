package com.dubois.aura.ui.theme

import androidx.compose.ui.graphics.Color
import com.dubois.aura.model.AuraTier

/**
 * Palette dérivée des maquettes Figma (thème sombre unique).
 * Regroupe toutes les couleurs custom qui ne rentrent pas naturellement
 * dans le [androidx.compose.material3.ColorScheme].
 */
object AuraPalette {
    // Fonds
    val Background       = Color(0xFF0B0912)
    val BackgroundGlow   = Color(0xFF1B1430) // halo violet en haut de l'écran
    val Surface          = Color(0xFF16141F) // cartes
    val SurfaceElevated  = Color(0xFF1E1B29) // cartes surlignées / champs
    val CardBorder       = Color(0xFF272233)
    val TabTrack         = Color(0xFF141220)

    // Marque
    val Primary              = Color(0xFF8B5CF6)
    val PrimaryGradientStart = Color(0xFF9B6BFF)
    val PrimaryGradientEnd   = Color(0xFF6D3EF0)
    val OnPrimary            = Color(0xFFFFFFFF)

    // Texte
    val TextPrimary   = Color(0xFFF3F1F9)
    val TextSecondary = Color(0xFF9A96AA)
    val TextMuted     = Color(0xFF6B6779)

    // Sémantique
    val Positive = Color(0xFF4ADE80)
    val Negative = Color(0xFFF26D6D)

    // Couleurs de tiers (utilisées pour le score, les anneaux d'avatar, la jauge)
    val TierSupreme  = Color(0xFFECA24E) // orange
    val TierDoree    = Color(0xFFA78BFA) // violet
    val TierVif      = Color(0xFF5B9BF5) // bleu
    val TierMontante = Color(0xFF4ADE80) // vert
    val TierBase     = Color(0xFF8A8698) // gris
}

/** Couleur associée à un tier de rang (côté UI). */
val AuraTier.color: Color
    get() = when (this) {
        AuraTier.AURA_SUPREME   -> AuraPalette.TierSupreme
        AuraTier.LUMIERE_DOREE  -> AuraPalette.TierDoree
        AuraTier.ECLAT_VIF      -> AuraPalette.TierVif
        AuraTier.LUEUR_MONTANTE -> AuraPalette.TierMontante
        AuraTier.ETINCELLE,
        AuraTier.AURA_NAISSANTE -> AuraPalette.TierBase
    }
