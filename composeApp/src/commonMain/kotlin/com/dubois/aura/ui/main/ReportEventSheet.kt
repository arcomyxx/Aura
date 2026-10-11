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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.EventCatalog
import com.dubois.aura.model.EventType
import com.dubois.aura.model.RankedUser
import com.dubois.aura.model.UserProfile
import com.dubois.aura.ui.components.InitialsAvatar
import com.dubois.aura.ui.components.SectionLabel
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.color
import com.dubois.aura.util.signed

/**
 * Contenu de la feuille « Signaler un événement ». Si [initialTarget] est nul
 * (ouverture depuis « + Signaler »), l'utilisateur choisit d'abord un ami.
 */
@Composable
fun ReportEventSheetContent(
    initialTarget: RankedUser?,
    friends: List<RankedUser>,
    onDismiss: () -> Unit,
    onSubmit: (target: UserProfile, type: EventType, description: String) -> Unit,
) {
    var target by remember { mutableStateOf(initialTarget) }
    var selectedType by remember { mutableStateOf<EventType?>(null) }
    var description by remember { mutableStateOf("") }

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
                    text = "Signaler un événement",
                    color = AuraPalette.TextPrimary,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
                Text(
                    text = target?.let { "Pour ${it.profile.displayName}" } ?: "Choisis un ami",
                    color = AuraPalette.TextSecondary,
                    fontSize = 14.sp,
                )
            }
            CloseButton(onDismiss)
        }
        Spacer(Modifier.height(20.dp))

        // Choix de l'ami (si non pré-ciblé)
        if (initialTarget == null) {
            SectionLabel("Pour qui ?")
            Spacer(Modifier.height(10.dp))
            if (friends.isEmpty()) {
                Text(
                    "Ajoute d'abord des amis pour pouvoir les signaler.",
                    color = AuraPalette.TextMuted,
                    fontSize = 13.sp,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(friends, key = { it.profile.uid }) { friend ->
                        FriendChip(
                            friend = friend,
                            selected = target?.profile?.uid == friend.profile.uid,
                            onClick = { target = friend },
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Grille des types d'événements (2 colonnes)
        EventCatalog.all.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { type ->
                    EventTypeCard(
                        type = type,
                        selected = selectedType?.id == type.id,
                        onClick = { selectedType = type },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Décris l'événement (optionnel)…", color = AuraPalette.TextMuted) },
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
        Spacer(Modifier.height(16.dp))

        val ready = target != null && selectedType != null
        Button(
            onClick = {
                val t = target ?: return@Button
                val type = selectedType ?: return@Button
                onSubmit(t.profile, type, description)
            },
            enabled = ready,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AuraPalette.Primary,
                contentColor = AuraPalette.OnPrimary,
                disabledContainerColor = AuraPalette.SurfaceElevated,
                disabledContentColor = AuraPalette.TextMuted,
            ),
        ) {
            Text("Valider l'événement", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
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

@Composable
private fun FriendChip(friend: RankedUser, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) AuraPalette.Primary else AuraPalette.CardBorder
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) AuraPalette.SurfaceElevated else AuraPalette.Surface)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InitialsAvatar(name = friend.profile.displayName, ringColor = friend.tier.color, size = 40)
        Spacer(Modifier.height(6.dp))
        Text(
            friend.profile.displayName.split(" ").first(),
            color = AuraPalette.TextPrimary,
            fontSize = 12.sp,
        )
    }
}

@Composable
internal fun EventTypeCard(
    type: EventType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = if (selected) AuraPalette.Primary else AuraPalette.CardBorder
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) AuraPalette.SurfaceElevated else AuraPalette.Surface)
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, border), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(type.emoji, fontSize = 20.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            type.label,
            color = AuraPalette.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${type.delta.signed()} aura",
            color = if (type.isPositive) AuraPalette.Positive else AuraPalette.Negative,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}
