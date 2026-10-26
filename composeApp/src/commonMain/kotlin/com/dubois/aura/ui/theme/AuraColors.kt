package com.dubois.aura.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.dubois.aura.model.AuraTier

/**
 * Palette « Super Saiyan » : fonds sombres chauds, accent or/orange, et
 * couleurs de tiers qui montent du blanc-gris au rouge-orange incandescent.
 * Regroupe toutes les couleurs custom qui ne rentrent pas naturellement
 * dans le [androidx.compose.material3.ColorScheme].
 */
object AuraPalette {
    // Fonds
    val Background       = Color(0xFF0C0906)
    val BackgroundGlow   = Color(0xFF2E1805) // halo orangé en haut de l'écran
    val Surface          = Color(0xFF17120D) // cartes
    val SurfaceElevated  = Color(0xFF211910) // cartes surlignées / champs
    val CardBorder       = Color(0xFF33261A)
    val TabTrack         = Color(0xFF14100B)

    // Marque — la couleur d'accent est choisie par l'utilisateur (thème perso).
    // C'est un état snapshot : toute lecture de Primary dans un composable se
    // recompose automatiquement quand l'accent change.
    val DefaultAccent = Color(0xFFFFA31A)

    private var accent by mutableStateOf(DefaultAccent)

    val Primary: Color get() = accent
    // Accent par défaut : dégradé jaune -> orange façon aura Super Saiyan.
    val PrimaryGradientStart: Color
        get() = if (accent == DefaultAccent) Color(0xFFFFD43B) else accent.adjustLightness(1.18f)
    val PrimaryGradientEnd: Color
        get() = if (accent == DefaultAccent) Color(0xFFFF7A00) else accent.adjustLightness(0.78f)
    /** Texte sur l'accent : sombre sur les accents clairs (jaune, or…), blanc sinon. */
    val OnPrimary: Color
        get() = if (accent.luminance() > 0.45f) Color(0xFF1A0F02) else Color(0xFFFFFFFF)

    /** Applique la couleur d'accent globale (ou revient au défaut si null). */
    fun setAccent(color: Color?) { accent = color ?: DefaultAccent }

    // Texte
    val TextPrimary   = Color(0xFFFBF4E8)
    val TextSecondary = Color(0xFFAB9D8A)
    val TextMuted     = Color(0xFF7A6D5D)

    // Sémantique
    val Positive = Color(0xFF4ADE80)
    val Negative = Color(0xFFF26D6D)

    // Couleurs de tiers (score, anneaux d'avatar, jauge, aura animée) :
    // plus le niveau monte, plus l'aura vire du blanc vers l'orange incandescent.
    val TierSupreme   = Color(0xFFFF5A1F) // rouge-orange incandescent
    val TierDoree     = Color(0xFFFF8C0A) // orange
    val TierVif       = Color(0xFFFFB31A) // or
    val TierMontante  = Color(0xFFFFD43B) // jaune
    val TierEtincelle = Color(0xFFFFE9A6) // jaune pâle
    val TierBase      = Color(0xFFB5AA9A) // blanc-gris
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
    Color(0xFFFFA31A), // or (défaut)
    Color(0xFFFFD43B), // jaune
    Color(0xFFFF5A1F), // rouge-orange
    Color(0xFF8B5CF6), // violet
    Color(0xFF5B9BF5), // bleu
    Color(0xFF22D3EE), // cyan
    Color(0xFF4ADE80), // vert
    Color(0xFFF26D6D), // rouge
    Color(0xFFEC4899), // rose
    Color(0xFFDDE3EA), // argent (ultra instinct)
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
        AuraTier.ETINCELLE      -> AuraPalette.TierEtincelle
        AuraTier.AURA_NAISSANTE -> AuraPalette.TierBase
    }
