package com.dubois.aura.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.UserProfile
import com.dubois.aura.ui.components.SectionLabel
import com.dubois.aura.ui.theme.AuraPalette

/**
 * Contenu de la feuille « Modifier le profil » : nom d'affichage et bio.
 */
@Composable
fun EditProfileSheetContent(
    profile: UserProfile?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, bio: String) -> Unit,
) {
    var name by remember { mutableStateOf(profile?.displayName ?: "") }
    var bio by remember { mutableStateOf(profile?.bio ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(bottom = 24.dp),
    ) {
        // En-tête
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Modifier le profil",
                    color = AuraPalette.TextPrimary,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
                Text(
                    text = profile?.email ?: "",
                    color = AuraPalette.TextSecondary,
                    fontSize = 14.sp,
                )
            }
            CloseButtonEdit(onDismiss)
        }
        Spacer(Modifier.height(20.dp))

        SectionLabel("Nom d'affichage")
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Ton nom", color = AuraPalette.TextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = editFieldColors(),
        )
        Spacer(Modifier.height(18.dp))

        SectionLabel("Bio")
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            placeholder = { Text("Quelques mots sur toi (optionnel)…", color = AuraPalette.TextMuted) },
            modifier = Modifier.fillMaxWidth().height(110.dp),
            shape = RoundedCornerShape(14.dp),
            colors = editFieldColors(),
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { onSubmit(name, bio) },
            enabled = name.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AuraPalette.Primary,
                contentColor = AuraPalette.OnPrimary,
                disabledContainerColor = AuraPalette.SurfaceElevated,
                disabledContentColor = AuraPalette.TextMuted,
            ),
        ) {
            Text("Enregistrer", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun editFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AuraPalette.TextPrimary,
    unfocusedTextColor = AuraPalette.TextPrimary,
    focusedContainerColor = AuraPalette.Surface,
    unfocusedContainerColor = AuraPalette.Surface,
    focusedBorderColor = AuraPalette.Primary,
    unfocusedBorderColor = AuraPalette.CardBorder,
    cursorColor = AuraPalette.Primary,
)

@Composable
private fun CloseButtonEdit(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(AuraPalette.SurfaceElevated)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", color = AuraPalette.TextSecondary, fontSize = 15.sp)
    }
}
