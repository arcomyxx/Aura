package com.dubois.aura.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.dubois.aura.model.AuraTier
import com.dubois.aura.ui.theme.color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/*
 * Aura animée façon Dragon Ball Z : des langues de flammes montent en continu
 * autour du contenu. Plus le score est élevé, plus l'aura est haute, rapide,
 * opaque et chaude (blanc-gris -> jaune -> or -> orange -> rouge-orange), avec
 * des étincelles à partir de « Lueur Montante » et des éclairs façon SSJ2 en
 * haut de « Lumière Dorée » / « Aura Suprême ».
 */

private const val MAX_LEVEL = 5f
private const val PI_F = PI.toFloat()

/** Niveau continu d'aura dans [0, 5] : index du tier + progression vers le suivant. */
fun auraLevel(score: Int): Float {
    val tier = AuraTier.forScore(score)
    val next = AuraTier.entries.getOrNull(tier.ordinal + 1) ?: return tier.ordinal.toFloat()
    val progress = ((score - tier.minScore).toFloat() / (next.minScore - tier.minScore)).coerceIn(0f, 1f)
    return tier.ordinal + progress
}

/** Couleur d'aura interpolée entre les couleurs de tiers. */
fun auraColorAt(level: Float): Color {
    val tiers = AuraTier.entries
    val i = floor(level).toInt().coerceIn(0, tiers.lastIndex)
    if (i == tiers.lastIndex) return tiers[i].color
    return lerp(tiers[i].color, tiers[i + 1].color, (level - i).coerceIn(0f, 1f))
}

private fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

private class AuraStyle(level: Float, compact: Boolean) {
    private val t = level / MAX_LEVEL
    val color = auraColorAt(level)
    val midColor = lerp(color, Color(0xFFFFE680), 0.55f)
    val coreColor = Color(0xFFFFF6D8)

    /** Extension de base de l'aura au-delà du contenu (fraction du rayon). */
    val reach = mix(0.16f, 0.5f, t) * if (compact) 0.5f else 1f
    /** Amplitude des langues de flammes. */
    val flicker = mix(0.05f, 0.2f, t) * if (compact) 0.55f else 1f
    /** Vitesse de montée des flammes. */
    val speed = mix(0.3f, 1.5f, t)
    val alpha = mix(0.35f, 0.92f, t)
    val layers = 1 + (if (level >= 1f) 1 else 0) + (if (level >= 3f) 1 else 0)
    val sparks = ((level - 1.5f) * 3f).toInt().coerceAtLeast(0).let { if (compact) it / 2 else it }
    /** Intervalle entre deux éclairs (s), 0 = pas d'éclairs. */
    val lightningEvery = if (level >= 4.5f) mix(2.4f, 0.9f, (level - 4.5f) / 0.5f) else 0f
    val points = if (compact) 56 else 110
    /** Hauteur max des flammes au-dessus du contenu (fraction du rayon). */
    val headroom = reach * 1.25f + flicker * 2.5f + 0.08f
}

/**
 * Place à réserver au-dessus d'un contenu de rayon 1 pour que les flammes de
 * l'aura ne soient pas coupées (fraction du rayon).
 */
fun auraHeadroom(score: Int, compact: Boolean = false): Float =
    AuraStyle(auraLevel(score), compact).headroom

/** Temps écoulé (s) qui avance à chaque frame, pour animer l'aura. */
@Composable
private fun rememberAuraTime(): State<Float> = produceState(0f) {
    val start = withInfiniteAnimationFrameNanos { it }
    while (true) {
        withInfiniteAnimationFrameNanos { value = ((it - start) / 1_000_000L).toFloat() / 1000f }
    }
}

/**
 * Dessine une aura de flammes animée autour du composant (derrière lui), plus
 * des étincelles et éclairs devant. [compact] réduit l'aura pour les petits
 * avatars dans les listes. Seul le dessin est invalidé à chaque frame (pas de
 * recomposition).
 */
fun Modifier.auraFlame(score: Int, compact: Boolean = false): Modifier = composed {
    val level = auraLevel(score)
    val style = remember(level, compact) { AuraStyle(level, compact) }
    val time by rememberAuraTime()
    val flame = remember { Path() }
    val bolt = remember { Path() }
    drawWithContent {
        drawAuraBack(style, time, flame)
        drawContent()
        drawAuraFront(style, time, bolt)
    }
}

private val TONGUES = floatArrayOf(5f, 8f, 13f)

private fun fract(v: Float) = v - floor(v)
private fun hash(n: Int) = fract(sin(n * 12.9898f) * 43758.547f)

/** Contour de flamme : corps ovale + langues pointues qui montent le long des côtés. */
private fun flamePath(path: Path, c: Offset, r: Float, k: Float, s: AuraStyle, time: Float, seed: Int) {
    path.reset()
    val base = r * (1f + s.reach * 0.35f) * k
    val pulse = 1f + 0.025f * sin(time * (1.5f + s.speed * 2f) + seed)
    val n = s.points
    for (j in 0 until n) {
        val th = -PI_F + 2f * PI_F * j / n // 0 = haut
        val phi = abs(th)
        val side = if (th >= 0f) 1f else -1f
        val w = ((1f + cos(phi)) / 2f).pow(1.6f) // 0 en bas, 1 en haut
        var spike = 0f
        TONGUES.forEachIndexed { idx, f ->
            val u = f * phi / PI_F + time * s.speed * (0.55f + 0.18f * idx) +
                seed * 0.37f + idx * 0.21f + side * 0.17f * sin(phi)
            val tri = (1f - abs(fract(u) - 0.5f) * 2f).coerceAtLeast(0f)
            spike = max(spike, tri * tri * tri)
        }
        var x = c.x + base * pulse * 0.97f * sin(th) * (1f - 0.15f * w)
        val y = c.y - base * pulse * cos(th) -
            r * k * (s.reach * 0.9f * w.pow(1.4f) + s.flicker * spike * (0.3f + 2.2f * w))
        // les langues se resserrent vers le centre en montant
        x -= (x - c.x) * 0.22f * spike * w
        if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
}

private fun DrawScope.drawAuraBack(s: AuraStyle, time: Float, path: Path) {
    val r = size.minDimension / 2f
    val c = center

    // Halo diffus
    val glowCenter = Offset(c.x, c.y - r * s.reach * 0.5f)
    val glowR = r * (1f + s.reach * 1.6f)
    drawCircle(
        brush = Brush.radialGradient(
            0f to s.color.copy(alpha = s.alpha * 0.5f),
            0.55f to s.color.copy(alpha = s.alpha * 0.22f),
            1f to Color.Transparent,
            center = glowCenter,
            radius = glowR,
        ),
        radius = glowR,
        center = glowCenter,
    )

    // Couches de flammes : extérieure (couleur du niveau) -> coeur clair
    val colors = listOf(s.color, s.midColor, s.coreColor)
    for (i in 0 until s.layers) {
        val k = 1f - i * 0.14f
        flamePath(path, c, r, k, s, time, seed = i)
        val a = s.alpha * (0.9f - i * 0.2f)
        drawPath(
            path,
            brush = Brush.radialGradient(
                0f to colors[i].copy(alpha = a * 0.3f),
                0.6f to colors[i].copy(alpha = a * 0.85f),
                1f to colors[i].copy(alpha = a),
                center = c,
                radius = r * k * (1f + s.headroom),
            ),
        )
    }
}

private fun DrawScope.drawAuraFront(s: AuraStyle, time: Float, bolt: Path) {
    val r = size.minDimension / 2f
    val c = center

    // Étincelles qui montent
    for (i in 0 until s.sparks) {
        val r1 = hash(i * 3 + 1)
        val r2 = hash(i * 3 + 2)
        val r3 = hash(i * 3 + 3)
        val p = fract(time * s.speed * (0.25f + 0.3f * r2) + r3)
        val x = c.x + (r1 * 2f - 1f) * r * (1f + s.reach * 0.6f) + sin(time * 3f + i) * r * 0.04f
        val y = c.y + r * 0.7f - p * r * (1.7f + s.headroom * 1.6f)
        // pas d'étincelles devant le contenu (score, initiales)
        val dist = (Offset(x, y) - c).getDistance() / r
        val a = sin(p * PI_F) * s.alpha * ((dist - 0.95f) / 0.2f).coerceIn(0f, 1f)
        if (a <= 0f) continue
        val rad = (r * 0.035f * (0.6f + r2)).coerceIn(1.dp.toPx(), 2.5.dp.toPx())
        drawCircle(s.midColor.copy(alpha = a * 0.35f), radius = rad * 2.6f, center = Offset(x, y))
        drawCircle(s.coreColor.copy(alpha = a), radius = rad, center = Offset(x, y))
    }

    // Éclairs (façon Super Saiyan 2), en flash bref
    if (s.lightningEvery > 0f) {
        val cycle = floor(time / s.lightningEvery).toInt()
        val local = time / s.lightningEvery - cycle
        if (local < 0.16f) {
            val flash = 1f - local / 0.16f
            val side = if (cycle % 2 == 0) -1f else 1f
            var x = c.x + side * r * (0.7f + 0.25f * hash(cycle))
            var y = c.y + r * 0.55f
            bolt.reset()
            bolt.moveTo(x, y)
            repeat(6) { j ->
                y -= r * (0.22f + 0.08f * hash(cycle * 7 + j))
                x += side * r * (if (j % 2 == 0) 0.16f else -0.12f) + (hash(cycle * 13 + j) - 0.5f) * r * 0.08f
                bolt.lineTo(x, y)
            }
            val w = max(1.5.dp.toPx(), r * 0.04f)
            drawPath(
                bolt,
                Color(0xFF9AD8FF).copy(alpha = 0.45f * flash),
                style = Stroke(w * 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                bolt,
                Color(0xFFF2FBFF).copy(alpha = flash),
                style = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
