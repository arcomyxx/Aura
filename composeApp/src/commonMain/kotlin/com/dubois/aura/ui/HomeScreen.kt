package com.dubois.aura.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.AuraTier
import com.dubois.aura.ui.components.auraFlame
import com.dubois.aura.ui.main.GradientButton
import com.dubois.aura.ui.theme.AuraPalette

/** Écran d'accueil hors connexion : logo à l'aura animée et accès connexion / inscription. */
@Composable
fun HomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Box(Modifier.fillMaxSize().auraScreenBackground()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            // Logo : orbe entouré de l'aura maximale
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .auraFlame(AuraTier.AURA_SUPREME.minScore)
                    .clip(CircleShape)
                    .background(AuraPalette.Background.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "A",
                    color = AuraPalette.Primary,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 56.sp,
                )
            }
            Spacer(Modifier.height(36.dp))
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
