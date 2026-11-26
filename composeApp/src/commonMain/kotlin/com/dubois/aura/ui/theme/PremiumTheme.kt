package com.dubois.aura.ui.theme

import androidx.compose.ui.graphics.Color

/** Couleurs exclusives Premium. */
object PremiumColors {
    // Badge ✦
    val BadgeGold      = Color(0xFFFFD700)
    val BadgeGoldLight = Color(0xFFFFF0A0)

    // Cadre avatar — dégradé métallique animé
    val FrameStart  = Color(0xFFFFD700)
    val FrameMid    = Color(0xFFFFF4B8)
    val FrameEnd    = Color(0xFFFFAA00)

    // Thèmes exclusifs
    val MetallicGold    = Color(0xFFCFB53B)
    val MetallicSilver  = Color(0xFFC0C0C0)
    val HoloPurple      = Color(0xFFB44FFF)
    val HoloCyan        = Color(0xFF00FFEE)
    val GradientRose    = Color(0xFFFF6EB4)
    val GradientIndigo  = Color(0xFF7B68EE)
}

/** Accents exclusifs proposés uniquement aux utilisateurs Premium. */
val PremiumAccentPresets: List<androidx.compose.ui.graphics.Color> = listOf(
    PremiumColors.MetallicGold,
    PremiumColors.MetallicSilver,
    PremiumColors.HoloPurple,
    PremiumColors.HoloCyan,
    PremiumColors.GradientRose,
    PremiumColors.GradientIndigo,
)
