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
 * les couleurs custom vivent dans [AuraPalette].
 */
private val AuraDarkColors = darkColorScheme(
    primary            = AuraPalette.Primary,
    onPrimary          = AuraPalette.OnPrimary,
    primaryContainer   = AuraPalette.PrimaryGradientEnd,
    onPrimaryContainer = AuraPalette.OnPrimary,
    background         = AuraPalette.Background,
    onBackground       = AuraPalette.TextPrimary,
    surface            = AuraPalette.Surface,
    onSurface          = AuraPalette.TextPrimary,
    surfaceVariant     = AuraPalette.SurfaceElevated,
    onSurfaceVariant   = AuraPalette.TextSecondary,
    outline            = AuraPalette.CardBorder,
    error              = AuraPalette.Negative,
)

@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AuraDarkColors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}
