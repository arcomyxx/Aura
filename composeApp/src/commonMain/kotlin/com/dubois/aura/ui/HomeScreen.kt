package com.dubois.aura.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aura.composeapp.generated.resources.Res
import aura.composeapp.generated.resources.ic_aura_flame
import com.dubois.aura.ui.main.GradientButton
import com.dubois.aura.ui.theme.AuraPalette
import org.jetbrains.compose.resources.painterResource

/** Écran d'accueil hors connexion : logo flamme animée et accès connexion / inscription. */
@Composable
fun HomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val infinite = rememberInfiniteTransition()

    // Luminosité oscillante (effet de brillance)
    val brightness by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    // Rayon du halo qui pulse
    val glowRadius by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )

    Box(Modifier.fillMaxSize().auraScreenBackground()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Image(
                painter = painterResource(Res.drawable.ic_aura_flame),
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .drawBehind {
                        // Halo orange qui pulse derrière la flamme
                        val radius = size.minDimension * glowRadius
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xAAFF6B00),
                                    Color(0x66FF3D00),
                                    Color(0x22FF1500),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width / 2f, size.height * 0.65f),
                                radius = radius,
                            ),
                            radius = radius,
                            center = Offset(size.width / 2f, size.height * 0.65f),
                        )
                    }
                    .graphicsLayer {
                        alpha = brightness.coerceIn(0f, 1f)
                    },
            )

            Spacer(Modifier.height(32.dp))
            Text(
                "Aura",
                color = AuraPalette.TextPrimary,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Fais monter ton aura, défie tes amis\net grimpe au classement.",
                color = AuraPalette.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1.3f))
            GradientButton(text = "Se connecter", onClick = onLoginClick)
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(BorderStroke(1.dp, AuraPalette.CardBorder), RoundedCornerShape(26.dp))
                    .clickable(onClick = onRegisterClick),
                contentAlignment = Alignment.Center,
            ) {
                Text("Créer un compte", color = AuraPalette.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
