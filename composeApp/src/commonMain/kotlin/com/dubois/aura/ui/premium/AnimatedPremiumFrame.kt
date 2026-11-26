package com.dubois.aura.ui.premium

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dubois.aura.ui.theme.PremiumColors
import kotlin.math.min

/**
 * Cadre animé doré autour de l'avatar des utilisateurs Premium.
 * Rotation d'un dégradé de lumière en continu.
 */
@Composable
fun AnimatedPremiumFrame(
    size: Dp = 64.dp,
    strokeWidth: Dp = 2.5.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "premiumFrame")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "frameRotation",
    )

    val shimmer by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "frameShimmer",
    )

    val frameColors = listOf(
        PremiumColors.FrameStart,
        PremiumColors.FrameMid.copy(alpha = shimmer),
        PremiumColors.FrameEnd,
        PremiumColors.FrameStart.copy(alpha = 0.4f),
        PremiumColors.FrameStart,
    )

    Box(
        modifier = modifier
            .size(size + strokeWidth * 2 + 4.dp)
            .drawBehind {
                val radius = min(this.size.width, this.size.height) / 2f
                rotate(rotation) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = frameColors,
                            center = Offset(this.size.width / 2f, this.size.height / 2f),
                        ),
                        radius = radius,
                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
