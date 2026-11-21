package com.dubois.aura.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.AuraTier
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.util.compact

/**
 * Jauge d'aura circulaire entourée d'une aura de flammes animée (voir
 * [auraFlame]) : anneaux concentriques, arc de progression coloré selon le
 * tier, et score compact au centre.
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
    padded: Boolean = true,
    flame: Boolean = true,
    arc: Boolean = true,
    name: String = "",
    photoBase64: String = "",
) {
    val tier = AuraTier.forScore(score)
    val next = AuraTier.entries.firstOrNull { it.minScore > tier.minScore }
    val progress = if (next == null) 1f else {
        ((score - tier.minScore).toFloat() / (next.minScore - tier.minScore)).coerceIn(0f, 1f)
    }

    val headroom = if (flame) diameter / 2f * auraHeadroom(score) else 0f
    val sidePadding = if (padded) 24.dp else 0.dp

    if (name.isNotEmpty()) {
        // Mode avatar : photo plein cercle + arc de progression autour + score sous la jauge
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = headroom.dp, start = sidePadding, end = sidePadding)
                    .size(diameter.dp)
                    .then(if (flame) Modifier.auraFlame(score) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.offset(y = (diameter * 0.08f).dp)) {
                InitialsAvatar(
                    name = name,
                    ringColor = tierColor,
                    size = (diameter * 0.82f).toInt(),
                    photoBase64 = photoBase64,
                )
                // Arc de progression par-dessus les bords de l'avatar
                if (arc) Canvas(Modifier.size(diameter.dp)) {
                    val stroke = size.minDimension * 0.055f
                    val inset = stroke / 2
                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                    val topLeft = Offset(inset, inset)
                    // Piste
                    drawArc(
                        color = AuraPalette.SurfaceElevated.copy(alpha = 0.6f),
                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    // Progression : gris→couleur du tier, sans cap au départ
                    rotate(-90f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colorStops = arrayOf(
                                    0f to AuraPalette.SurfaceElevated,
                                    progress.coerceAtLeast(0.01f) to tierColor,
                                    1f to tierColor,
                                )
                            ),
                            startAngle = 0f, sweepAngle = 360f * progress, useCenter = false,
                            topLeft = topLeft, size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Butt),
                        )
                    }
                    // Cap arrondi uniquement en bout d'arc
                    if (progress > 0.01f) {
                        val endAngle = (-90f + 360f * progress) * (PI / 180.0)
                        val arcR = size.minDimension / 2f - inset
                        drawCircle(
                            color = tierColor,
                            radius = stroke / 2f,
                            center = Offset(
                                center.x + arcR * cos(endAngle).toFloat(),
                                center.y + arcR * sin(endAngle).toFloat(),
                            ),
                        )
                    }
                }
                } // end offset Box
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = score.compact(),
                color = tierColor,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = (diameter * 0.20f).sp,
            )
        }
    } else {
        // Mode score : chiffre + "AURA" à l'intérieur de la jauge
        Box(
            modifier = modifier
                .then(
                    if (padded) Modifier.padding(top = headroom.dp, start = 24.dp, end = 24.dp)
                    else if (headroom > 0f) Modifier.padding(top = headroom.dp)
                    else Modifier
                )
                .size(diameter.dp)
                .then(if (flame) Modifier.auraFlame(score) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(diameter.dp)) {
                translate(top = size.minDimension * 0.08f) {
                scale(0.82f) {
                val stroke = size.minDimension * 0.045f
                val inset = stroke * 2
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                val topLeft = Offset(inset, inset)

                drawCircle(
                    color = AuraPalette.Background.copy(alpha = 0.88f),
                    radius = size.minDimension * 0.5f - stroke / 2,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tierColor.copy(alpha = 0.22f), Color.Transparent),
                        center = center,
                        radius = size.minDimension * 0.42f,
                    ),
                    radius = size.minDimension * 0.42f,
                )
                drawCircle(
                    color = AuraPalette.CardBorder,
                    radius = size.minDimension * 0.5f - stroke / 2,
                    style = Stroke(width = stroke * 0.5f),
                )
                drawArc(
                    color = AuraPalette.SurfaceElevated,
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                // Progression : gris→couleur du tier, sans cap au départ
                rotate(-90f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colorStops = arrayOf(
                                0f to AuraPalette.SurfaceElevated,
                                progress.coerceAtLeast(0.01f) to tierColor,
                                1f to tierColor,
                            )
                        ),
                        startAngle = 0f, sweepAngle = 360f * progress, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                }
                // Cap arrondi uniquement en bout d'arc
                if (progress > 0.01f) {
                    val endAngle = (-90f + 360f * progress) * (PI / 180.0)
                    val arcR = size.minDimension / 2f - inset
                    drawCircle(
                        color = tierColor,
                        radius = stroke / 2f,
                        center = Offset(
                            center.x + arcR * cos(endAngle).toFloat(),
                            center.y + arcR * sin(endAngle).toFloat(),
                        ),
                    )
                }
                drawCircle(
                    color = tierColor.copy(alpha = 0.25f),
                    radius = size.minDimension * 0.34f,
                    style = Stroke(width = 1.dp.toPx()),
                )
                } // end scale
                } // end translate
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
}
