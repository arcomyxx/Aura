package com.dubois.aura.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.UserProfile
import com.dubois.aura.ui.components.InitialsAvatar
import com.dubois.aura.ui.components.SectionLabel
import com.dubois.aura.ui.components.clickableNoRipple
import com.dubois.aura.ui.theme.AccentPresets
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.colorFromHex
import com.dubois.aura.ui.theme.toHex
import com.dubois.aura.util.encodeImageBase64
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.compressImage
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch

/**
 * Contenu de la feuille « Modifier le profil » : photo, nom d'affichage et bio.
 * La photo est choisie via le sélecteur natif, réduite en miniature puis encodée
 * en base64 (stockée dans le profil Firestore).
 */
@Composable
fun EditProfileSheetContent(
    profile: UserProfile?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, bio: String, photoUrl: String, themeColor: String) -> Unit,
) {
    var name by remember { mutableStateOf(profile?.displayName ?: "") }
    var bio by remember { mutableStateOf(profile?.bio ?: "") }
    var photo by remember { mutableStateOf(profile?.photoUrl ?: "") }
    var themeHex by remember { mutableStateOf(profile?.themeColor ?: "") }
    var processing by remember { mutableStateOf(false) }

    val accent = colorFromHex(themeHex) ?: AuraPalette.DefaultAccent

    val scope = rememberCoroutineScope()
    val pickImage = rememberFilePickerLauncher(type = FileKitType.Image) { file: PlatformFile? ->
        if (file != null) {
            processing = true
            scope.launch {
                try {
                    val compressed = FileKit.compressImage(
                        file = file,
                        imageFormat = ImageFormat.JPEG,
                        quality = 80,
                        maxWidth = 256,
                        maxHeight = 256,
                    )
                    photo = encodeImageBase64(compressed)
                } catch (e: Exception) {
                    // On garde la photo précédente en cas d'échec.
                } finally {
                    processing = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
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

        // Photo de profil
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InitialsAvatar(
                name = name,
                ringColor = accent,
                size = 96,
                fontSize = 32,
                photoBase64 = photo,
                modifier = Modifier.clickableNoRipple { if (!processing) pickImage.launch() },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (processing) "Traitement…" else "Changer la photo",
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickableNoRipple { if (!processing) pickImage.launch() }
                    .padding(4.dp),
            )
            if (photo.isNotBlank()) {
                Text(
                    text = "Retirer la photo",
                    color = AuraPalette.TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickableNoRipple { photo = "" }
                        .padding(4.dp),
                )
            }
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
        Spacer(Modifier.height(18.dp))

        // Couleur de thème
        SectionLabel("Couleur du thème")
        Spacer(Modifier.height(12.dp))
        val selectedHex = colorFromHex(themeHex)?.toHex()
        AccentPresets.chunked(5).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowColors.forEach { preset ->
                    ColorSwatch(
                        color = preset,
                        selected = selectedHex == preset.toHex(),
                        onClick = { themeHex = preset.toHex() },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .border(BorderStroke(1.dp, AuraPalette.CardBorder), CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = themeHex,
                onValueChange = { themeHex = it.take(7) },
                placeholder = { Text("#8B5CF6", color = AuraPalette.TextMuted) },
                singleLine = true,
                prefix = { Text("#", color = AuraPalette.TextMuted) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                isError = themeHex.isNotBlank() && colorFromHex(themeHex) == null,
                colors = editFieldColors(),
            )
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onSubmit(name, bio, photo, colorFromHex(themeHex)?.toHex() ?: "") },
            enabled = name.trim().isNotEmpty() && !processing,
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
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                BorderStroke(
                    if (selected) 3.dp else 1.dp,
                    if (selected) AuraPalette.TextPrimary else AuraPalette.CardBorder,
                ),
                CircleShape,
            )
            .clickable(onClick = onClick),
    )
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
