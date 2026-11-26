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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.UserProfile
import com.dubois.aura.ui.components.InitialsAvatar
import com.dubois.aura.ui.components.SectionLabel
import com.dubois.aura.ui.components.blockTouchesBehind
import com.dubois.aura.ui.components.clickableNoRipple
import com.dubois.aura.ui.premium.PremiumBadge
import com.dubois.aura.ui.theme.AccentPresets
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.PremiumAccentPresets
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

@Composable
fun AccountScreen(
    profile: UserProfile?,
    isPremium: Boolean = false,
    onUpgradePremium: () -> Unit = {},
    onSave: (name: String, bio: String, photoUrl: String, themeColor: String) -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf(profile?.displayName ?: "") }
    var bio by remember { mutableStateOf(profile?.bio ?: "") }
    var photo by remember { mutableStateOf(profile?.photoUrl ?: "") }
    var themeHex by remember { mutableStateOf(profile?.themeColor ?: "") }
    var processing by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val accent = colorFromHex(themeHex) ?: AuraPalette.DefaultAccent
    val uriHandler = LocalUriHandler.current
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
                } catch (_: Exception) {
                } finally {
                    processing = false
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = AuraPalette.Surface,
            title = { Text("Se déconnecter ?", color = AuraPalette.TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Tu pourras te reconnecter à tout moment.", color = AuraPalette.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showLogoutConfirm = false; onLogout() }) {
                    Text("Se déconnecter", color = AuraPalette.Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Annuler", color = AuraPalette.TextMuted)
                }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = AuraPalette.Surface,
            title = { Text("Supprimer le compte", color = AuraPalette.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Cette action est irréversible. Ton profil, tes events et ton score seront supprimés définitivement.",
                    color = AuraPalette.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDeleteAccount() }) {
                    Text("Supprimer", color = AuraPalette.Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler", color = AuraPalette.TextMuted)
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blockTouchesBehind()
            .background(AuraPalette.Background)
            .background(
                Brush.radialGradient(
                    colors = listOf(AuraPalette.BackgroundGlow, AuraPalette.Background),
                    center = Offset(0.5f * 1000f, 0f),
                    radius = 900f,
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .imePadding(),
        ) {
            // En-tête
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            ) {
                BackButton(onBack)
                Spacer(Modifier.width(14.dp))
                Text(
                    "Mon compte",
                    color = AuraPalette.TextPrimary,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))

                // Photo
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
                    text = profile?.email ?: "",
                    color = AuraPalette.TextMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (processing) "Traitement…" else "Changer la photo",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickableNoRipple { if (!processing) pickImage.launch() }.padding(4.dp),
                )
                if (photo.isNotBlank()) {
                    Text(
                        text = "Retirer la photo",
                        color = AuraPalette.TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.clickableNoRipple { photo = "" }.padding(4.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))

                // Nom
                SectionLabel("Nom d'affichage", Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Ton nom", color = AuraPalette.TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = accountFieldColors(),
                )
                Spacer(Modifier.height(18.dp))

                // Bio
                SectionLabel("Bio", Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    placeholder = { Text("Quelques mots sur toi (optionnel)…", color = AuraPalette.TextMuted) },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = accountFieldColors(),
                )
                Spacer(Modifier.height(18.dp))

                // Couleur de thème
                SectionLabel("Couleur du thème", Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                val selectedHex = colorFromHex(themeHex)?.toHex()
                AccentPresets.chunked(5).forEach { rowColors ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
                    SectionLabel("Exclusif")
                    Spacer(Modifier.width(6.dp))
                    PremiumBadge()
                }
                Spacer(Modifier.height(10.dp))
                PremiumAccentPresets.chunked(5).forEach { rowColors ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        rowColors.forEach { preset ->
                            ColorSwatch(
                                color = preset,
                                selected = selectedHex == preset.toHex(),
                                onClick = { if (isPremium) themeHex = preset.toHex() else onUpgradePremium() },
                                locked = !isPremium,
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
                        placeholder = { Text("#FFA31A", color = AuraPalette.TextMuted) },
                        singleLine = true,
                        prefix = { Text("#", color = AuraPalette.TextMuted) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        isError = themeHex.isNotBlank() && colorFromHex(themeHex) == null,
                        colors = accountFieldColors(),
                    )
                }
                Spacer(Modifier.height(24.dp))

                // Enregistrer
                Button(
                    onClick = { onSave(name, bio, photo, colorFromHex(themeHex)?.toHex() ?: "") },
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
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Politique de confidentialité · Mentions légales",
                    color = AuraPalette.TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple { uriHandler.openUri("https://aura-7d0fb.web.app/privacy") },
                )

                Spacer(Modifier.height(48.dp))

                // Zone dangereuse
                Button(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraPalette.Negative.copy(alpha = 0.12f),
                        contentColor = AuraPalette.Negative,
                    ),
                ) {
                    Text("Se déconnecter", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Supprimer le compte",
                        color = AuraPalette.Negative.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun accountFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = AuraPalette.TextPrimary,
    unfocusedTextColor = AuraPalette.TextPrimary,
    focusedContainerColor = AuraPalette.Surface,
    unfocusedContainerColor = AuraPalette.Surface,
    focusedBorderColor = AuraPalette.Primary,
    unfocusedBorderColor = AuraPalette.CardBorder,
    cursorColor = AuraPalette.Primary,
)
