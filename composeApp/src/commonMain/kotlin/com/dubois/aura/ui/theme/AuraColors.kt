package com.dubois.aura.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    // Marque — la couleur d'accent est choisie par l'utilisateur (thème perso).
    // C'est un état snapshot : toute lecture de Primary dans un composable se
    // recompose automatiquement quand l'accent change.
    val DefaultAccent = Color(0xFF8B5CF6)

    private var accent by mutableStateOf(DefaultAccent)

    val Primary: Color get() = accent
    val PrimaryGradientStart: Color get() = accent.adjustLightness(1.18f)
    val PrimaryGradientEnd: Color get() = accent.adjustLightness(0.78f)
    val OnPrimary            = Color(0xFFFFFFFF)

    /** Applique la couleur d'accent globale (ou revient au défaut si null). */
    fun setAccent(color: Color?) { accent = color ?: DefaultAccent }

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

/** Éclaircit/assombrit une couleur (facteur > 1 = plus clair). */
fun Color.adjustLightness(factor: Float): Color = Color(
    red = (red * factor).coerceIn(0f, 1f),
    green = (green * factor).coerceIn(0f, 1f),
    blue = (blue * factor).coerceIn(0f, 1f),
    alpha = alpha,
)

/** Couleurs d'accent prédéfinies proposées dans l'éditeur de profil. */
val AccentPresets: List<Color> = listOf(
    Color(0xFF8B5CF6), // violet (défaut)
    Color(0xFF5B9BF5), // bleu
    Color(0xFF22D3EE), // cyan
    Color(0xFF4ADE80), // vert
    Color(0xFFA3E635), // lime
    Color(0xFFECA24E), // orange
    Color(0xFFF59E0B), // ambre
    Color(0xFFF26D6D), // rouge
    Color(0xFFEC4899), // rose
    Color(0xFFF472B6), // rose clair
)

/**
 * Convertit une chaîne hex ("#RRGGBB", "RRGGBB" ou "AARRGGBB") en [Color],
 * ou null si vide/invalide.
 */
fun colorFromHex(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return null
    return try {
        val value = clean.toLong(16)
        val argb = if (clean.length == 6) 0xFF000000L or value else value
        Color(argb.toInt())
    } catch (e: NumberFormatException) {
        null
    }
}

/** Représentation "RRGGBB" (sans alpha) d'une couleur, pour stockage Firestore. */
fun Color.toHex(): String {
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    fun two(v: Int): String = v.toString(16).uppercase().padStart(2, '0')
    return two(r) + two(g) + two(b)
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
