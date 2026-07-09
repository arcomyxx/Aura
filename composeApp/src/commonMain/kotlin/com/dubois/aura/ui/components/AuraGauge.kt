package com.dubois.aura.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.AuraTier
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.util.compact

/**
 * Jauge d'aura circulaire : anneaux concentriques, arc de progression coloré
 * selon le tier, et score compact au centre.
 *
 * @param score score d'aura courant
 * @param tierColor couleur du tier (pour l'arc et le score)
 */
@Composable
fun AuraGauge(
    score: Int,
    tierColor: Color,
    modifier: Modifier = Modifier,
    diameter: Int = 200,
) {
    val tier = AuraTier.forScore(score)
    val next = AuraTier.entries.firstOrNull { it.minScore > tier.minScore }
    val progress = if (next == null) 1f else {
        ((score - tier.minScore).toFloat() / (next.minScore - tier.minScore)).coerceIn(0f, 1f)
    }

    Box(modifier = modifier.size(diameter.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter.dp)) {
            val stroke = size.minDimension * 0.045f
            val inset = stroke * 2
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // Halo central
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tierColor.copy(alpha = 0.18f), Color.Transparent),
                    center = center,
                    radius = size.minDimension * 0.42f,
                ),
                radius = size.minDimension * 0.42f,
            )

            // Anneau externe discret
            drawCircle(
                color = AuraPalette.CardBorder,
                radius = size.minDimension * 0.5f - stroke / 2,
                style = Stroke(width = stroke * 0.5f),
            )
            // Piste de l'arc
            drawArc(
                color = AuraPalette.SurfaceElevated,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Arc de progression
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(tierColor.copy(alpha = 0.6f), tierColor, tierColor)
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Anneau interne discret
            drawCircle(
                color = tierColor.copy(alpha = 0.25f),
                radius = size.minDimension * 0.34f,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = score.compact(),
                color = tierColor,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = (diameter * 0.22f).sp,
            )
            Text(
                text = "AURA",
                color = AuraPalette.TextMuted,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
