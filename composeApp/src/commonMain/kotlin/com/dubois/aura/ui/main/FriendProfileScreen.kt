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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.AuraEvent
import com.dubois.aura.model.RankedUser
import com.dubois.aura.ui.components.AuraCard
import com.dubois.aura.ui.components.AuraGauge
import com.dubois.aura.ui.components.InitialsAvatar
import com.dubois.aura.ui.components.SectionLabel
import com.dubois.aura.ui.components.clickableNoRipple
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.color
import com.dubois.aura.ui.theme.colorFromHex
import com.dubois.aura.util.grouped
import com.dubois.aura.util.relativeTime
import com.dubois.aura.util.signed
import kotlin.time.Clock

/**
 * Profil d'un ami : jauge, palier, rang et historique de ses événements.
 * Pour les signalements dont l'utilisateur courant est l'auteur, des actions
 * « Modifier » et « Supprimer » sont proposées.
 */
@Composable
fun FriendProfileScreen(
    friend: RankedUser,
    events: List<AuraEvent>,
    myUid: String?,
    onBack: () -> Unit,
    onEditEvent: (AuraEvent) -> Unit,
    onDeleteEvent: (AuraEvent) -> Unit,
) {
    val tierColor = friend.tier.color
    val accent = colorFromHex(friend.profile.themeColor) ?: tierColor
    val now = Clock.System.now().toEpochMilliseconds()
    var pendingDelete by remember { mutableStateOf<AuraEvent?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraPalette.Background)
            .background(
                Brush.radialGradient(
                    colors = listOf(AuraPalette.BackgroundGlow, AuraPalette.Background),
                    center = Offset(0.5f * 1000f, 0f),
                    radius = 900f,
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(horizontal = 20.dp),
        ) {
            // En-tête : retour + nom
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    SectionLabel("Profil")
                    Text(
                        text = friend.profile.displayName.ifBlank { "(sans nom)" },
                        color = AuraPalette.TextPrimary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                    )
                }
                InitialsAvatar(
                    name = friend.profile.displayName,
                    ringColor = accent,
                    size = 48,
                    photoBase64 = friend.profile.photoUrl,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Spacer(Modifier.height(16.dp))
                    AuraGauge(score = friend.profile.auraScore, tierColor = tierColor)
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = friend.tier.displayName,
                        color = tierColor,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Rang #${friend.rank} · ${friend.profile.auraScore.grouped()} aura",
                        color = AuraPalette.TextSecondary,
                        fontSize = 14.sp,
                    )
                    if (friend.profile.bio.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = friend.profile.bio,
                            color = AuraPalette.TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    SectionLabel("Historique", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                }

                if (events.isEmpty()) {
                    item {
                        Text(
                            text = "Aucun événement pour l'instant.",
                            color = AuraPalette.TextMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                } else {
                    items(events, key = { it.id }) { event ->
                        FriendEventRow(
                            event = event,
                            now = now,
                            mine = myUid != null && event.authorUid == myUid,
                            onEdit = { onEditEvent(event) },
                            onDelete = { pendingDelete = event },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // Confirmation de suppression
    pendingDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = AuraPalette.SurfaceElevated,
            title = { Text("Supprimer le signalement ?", color = AuraPalette.TextPrimary) },
            text = {
                Text(
                    "« ${event.description.ifBlank { event.label }} » (${event.delta.signed()} aura) sera retiré du score de ${friend.profile.displayName}.",
                    color = AuraPalette.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteEvent(event)
                    pendingDelete = null
                }) {
                    Text("Supprimer", color = AuraPalette.Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Annuler", color = AuraPalette.TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun FriendEventRow(
    event: AuraEvent,
    now: Long,
    mine: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AuraCard(Modifier.fillMaxWidth(), highlighted = mine) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                InitialsAvatar(
                    name = event.authorName,
                    ringColor = AuraPalette.TierBase,
                    size = 40,
                    fontSize = 13,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.authorName.ifBlank { "Anonyme" },
                            color = AuraPalette.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = relativeTime(event.createdAt, now),
                            color = AuraPalette.TextMuted,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${event.emoji} ${event.description.ifBlank { event.label }}",
                        color = AuraPalette.TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
                val positive = event.delta >= 0
                Text(
                    text = "${if (positive) "↗" else "↘"}${event.delta.signed()}",
                    color = if (positive) AuraPalette.Positive else AuraPalette.Negative,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }

            if (mine) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionChip("Modifier", AuraPalette.Primary, onEdit)
                    ActionChip("Supprimer", AuraPalette.Negative, onDelete)
                }
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AuraPalette.SurfaceElevated)
            .border(BorderStroke(1.dp, AuraPalette.CardBorder), CircleShape)
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("←", color = AuraPalette.TextPrimary, fontSize = 18.sp)
    }
}
