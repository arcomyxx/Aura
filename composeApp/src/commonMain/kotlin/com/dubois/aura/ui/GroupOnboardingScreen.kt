package com.dubois.aura.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.ui.theme.AuraPalette

@Composable
fun GroupOnboardingScreen(
    onCreateGroup: (String) -> Unit,
    onSkip: () -> Unit,
) {
    var groupName by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraPalette.Background)
            .background(
                Brush.radialGradient(
                    colors = listOf(AuraPalette.BackgroundGlow, AuraPalette.Background),
                    center = Offset(500f, 0f),
                    radius = 900f,
                )
            )
            .safeContentPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🔥", fontSize = 56.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Crée ton groupe",
                color = AuraPalette.TextPrimary,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "L'aura prend tout son sens entre amis.\nCrée un groupe pour avoir un vrai classement.",
                color = AuraPalette.TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(36.dp))

            if (showCreate) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text("Nom du groupe…", color = AuraPalette.TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AuraPalette.TextPrimary,
                        unfocusedTextColor = AuraPalette.TextPrimary,
                        focusedContainerColor = AuraPalette.Surface,
                        unfocusedContainerColor = AuraPalette.Surface,
                        focusedBorderColor = AuraPalette.Primary,
                        unfocusedBorderColor = AuraPalette.CardBorder,
                        cursorColor = AuraPalette.Primary,
                    ),
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val name = groupName.trim()
                        if (name.isNotEmpty()) onCreateGroup(name)
                    },
                    enabled = groupName.trim().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraPalette.Primary,
                        contentColor = AuraPalette.OnPrimary,
                        disabledContainerColor = AuraPalette.SurfaceElevated,
                        disabledContentColor = AuraPalette.TextMuted,
                    ),
                ) {
                    Text("Créer le groupe", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showCreate = false },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AuraPalette.CardBorder),
                ) {
                    Text("Retour", color = AuraPalette.TextSecondary, fontSize = 15.sp)
                }
            } else {
                Button(
                    onClick = { showCreate = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraPalette.Primary,
                        contentColor = AuraPalette.OnPrimary,
                    ),
                ) {
                    Text("Créer un groupe", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Tu pourras aussi rejoindre un groupe existant\nvia invitation une fois connecté.",
                    color = AuraPalette.TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
            }
        }

        // Lien "Passer" ancré en bas
        Text(
            text = "Passer pour l'instant →",
            color = AuraPalette.TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSkip,
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
