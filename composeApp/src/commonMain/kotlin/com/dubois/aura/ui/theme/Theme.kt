package com.dubois.aura.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Thème sombre unique de l'application, calé sur les maquettes Figma.
 * Les couleurs Material servent aux composants standard (champs, sheet…) ;
 * les couleurs custom vivent dans [AuraPalette]. Le schéma est recalculé dans
 * le composable pour suivre la couleur d'accent choisie par l'utilisateur.
 */
@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary            = AuraPalette.Primary,
        onPrimary          = AuraPalette.OnPrimary,
        primaryContainer   = AuraPalette.PrimaryGradientEnd,
        onPrimaryContainer = AuraPalette.OnPrimary,
        secondary          = AuraPalette.PrimaryGradientStart,
        onSecondary        = AuraPalette.OnPrimary,
        secondaryContainer = AuraPalette.SurfaceElevated,
        onSecondaryContainer = AuraPalette.TextPrimary,
        tertiary           = AuraPalette.TierDoree,
        onTertiary         = AuraPalette.OnPrimary,
        tertiaryContainer  = AuraPalette.Surface,
        onTertiaryContainer = AuraPalette.TextPrimary,
        background         = AuraPalette.Background,
        onBackground       = AuraPalette.TextPrimary,
        surface            = AuraPalette.Surface,
        onSurface          = AuraPalette.TextPrimary,
        surfaceVariant     = AuraPalette.SurfaceElevated,
        onSurfaceVariant   = AuraPalette.TextSecondary,
        outline            = AuraPalette.CardBorder,
        outlineVariant     = AuraPalette.CardBorder,
        error              = AuraPalette.Negative,
        onError            = AuraPalette.TextPrimary,
        errorContainer     = AuraPalette.Surface,
        onErrorContainer   = AuraPalette.Negative,
        inversePrimary     = AuraPalette.PrimaryGradientEnd,
        inverseSurface     = AuraPalette.TextPrimary,
        inverseOnSurface   = AuraPalette.Background,
        scrim              = AuraPalette.Background,
        surfaceTint        = AuraPalette.Primary,
    )
    MaterialTheme(colorScheme = colors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}
