package com.dubois.aura.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dubois.aura.ui.theme.AuraPalette

/**
 * Pictogrammes dessinés au trait. Les glyphes texte (« ← », « + », « ✕ »)
 * ne sont pas centrés de la même façon selon les polices : on les dessine
 * pour qu'ils tombent pile au centre de leur bouton.
 */
enum class AuraIcon { Back, Forward, Plus, Close }

@Composable
fun AuraIconGlyph(icon: AuraIcon, tint: Color, modifier: Modifier = Modifier, size: Int = 18) {
    Canvas(modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            when (icon) {
                AuraIcon.Back -> {
                    moveTo(w * 0.92f, h / 2)
                    lineTo(w * 0.08f, h / 2)
                    moveTo(w * 0.45f, h * 0.12f)
                    lineTo(w * 0.08f, h / 2)
                    lineTo(w * 0.45f, h * 0.88f)
                }
                AuraIcon.Forward -> {
                    moveTo(w * 0.08f, h / 2)
                    lineTo(w * 0.92f, h / 2)
                    moveTo(w * 0.55f, h * 0.12f)
                    lineTo(w * 0.92f, h / 2)
                    lineTo(w * 0.55f, h * 0.88f)
                }
                AuraIcon.Plus -> {
                    moveTo(w / 2, h * 0.1f)
                    lineTo(w / 2, h * 0.9f)
                    moveTo(w * 0.1f, h / 2)
                    lineTo(w * 0.9f, h / 2)
                }
                AuraIcon.Close -> {
                    moveTo(w * 0.15f, h * 0.15f)
                    lineTo(w * 0.85f, h * 0.85f)
                    moveTo(w * 0.85f, h * 0.15f)
                    lineTo(w * 0.15f, h * 0.85f)
                }
            }
        }
        drawPath(path, tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Bouton rond avec pictogramme centré (retour, ajout, fermeture…). */
@Composable
fun IconCircleButton(
    icon: AuraIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 44,
    iconSize: Int = 18,
    tint: Color = AuraPalette.TextPrimary,
    bordered: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(AuraPalette.SurfaceElevated)
            .then(if (bordered) Modifier.border(BorderStroke(1.dp, AuraPalette.CardBorder), CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AuraIconGlyph(icon, tint, size = iconSize)
    }
}
