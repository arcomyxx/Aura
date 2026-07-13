package com.dubois.aura.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.FriendRequest
import com.dubois.aura.model.RankedUser
import com.dubois.aura.model.UserProfile
import com.dubois.aura.ui.components.AuraCard
import com.dubois.aura.ui.components.InitialsAvatar
import com.dubois.aura.ui.components.SectionLabel
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.color
import com.dubois.aura.ui.theme.colorFromHex
import com.dubois.aura.util.grouped

@Composable
fun FriendsTab(
    friends: List<RankedUser>,
    searchResults: List<UserProfile>,
    requests: List<FriendRequest>,
    searching: Boolean,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSendRequest: (UserProfile) -> Unit,
    onAccept: (FriendRequest) -> Unit,
    onDecline: (FriendRequest) -> Unit,
    onReport: (RankedUser) -> Unit,
    onOpenProfile: (RankedUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {

        // ── Recherche ────────────────────────────────────────────
        item {
            SectionLabel("Ajouter un ami")
            Spacer(Modifier.height(10.dp))
            SearchBar(searching = searching, onSearch = onSearch, onClear = onClearSearch)
            Spacer(Modifier.height(if (searchResults.isNotEmpty()) 14.dp else 22.dp))
        }
        if (searchResults.isNotEmpty()) {
            items(searchResults, key = { "search_${it.uid}" }) { user ->
                SearchResultRow(user, onAdd = { onSendRequest(user) })
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(10.dp)) }
        }

        // ── Demandes reçues ──────────────────────────────────────
        if (requests.isNotEmpty()) {
            item {
                SectionLabel("Demandes reçues · ${requests.size}")
                Spacer(Modifier.height(10.dp))
            }
            items(requests, key = { "req_${it.id}" }) { request ->
                RequestRow(
                    request = request,
                    onAccept = { onAccept(request) },
                    onDecline = { onDecline(request) },
                )
                Spacer(Modifier.height(10.dp))
            }
            item { Spacer(Modifier.height(12.dp)) }
        }

        // ── Liste d'amis ─────────────────────────────────────────
        item {
            SectionLabel("Mes amis · ${friends.size}")
            Spacer(Modifier.height(14.dp))
        }
        if (friends.isEmpty()) {
            item {
                Text(
                    text = "Aucun ami pour l'instant. Recherche quelqu'un ci-dessus pour l'ajouter.",
                    color = AuraPalette.TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        } else {
            items(friends, key = { it.profile.uid }) { friend ->
                FriendRow(
                    friend,
                    onReport = { onReport(friend) },
                    onOpenProfile = { onOpenProfile(friend) },
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SearchBar(
    searching: Boolean,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.isBlank()) onClear()
            },
            placeholder = { Text("Nom ou email", color = AuraPalette.TextMuted) },
            singleLine = true,
            modifier = Modifier.weight(1f),
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
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = { if (query.isNotBlank()) onSearch(query.trim()) },
            enabled = query.isNotBlank() && !searching,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AuraPalette.Primary,
                contentColor = AuraPalette.OnPrimary,
                disabledContainerColor = AuraPalette.SurfaceElevated,
                disabledContentColor = AuraPalette.TextMuted,
            ),
        ) {
            if (searching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = AuraPalette.OnPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Chercher", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SearchResultRow(user: UserProfile, onAdd: () -> Unit) {
    val tierColor = com.dubois.aura.model.AuraTier.forScore(user.auraScore).color
    AuraCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InitialsAvatar(name = user.displayName, ringColor = tierColor, size = 42)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    user.displayName.ifBlank { "(sans nom)" },
                    color = AuraPalette.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(user.email, color = AuraPalette.TextMuted, fontSize = 12.sp)
            }
            PillButton("Ajouter", onClick = onAdd)
        }
    }
}

@Composable
private fun RequestRow(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AuraCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(name = request.fromName, ringColor = AuraPalette.Primary, size = 42)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        request.fromName.ifBlank { request.fromEmail },
                        color = AuraPalette.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(request.fromEmail, color = AuraPalette.TextMuted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                GhostButton("Refuser", onClick = onDecline, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                PillButton("Accepter", onClick = onAccept, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FriendRow(friend: RankedUser, onReport: () -> Unit, onOpenProfile: () -> Unit) {
    val tierColor = friend.tier.color
    val accent = colorFromHex(friend.profile.themeColor)
    AuraCard(Modifier.fillMaxWidth(), accent = accent) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenProfile).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InitialsAvatar(
                name = friend.profile.displayName,
                ringColor = accent ?: tierColor,
                size = 46,
                photoBase64 = friend.profile.photoUrl,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = friend.profile.displayName.ifBlank { "(sans nom)" },
                    color = AuraPalette.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(friend.tier.displayName, color = AuraPalette.TextSecondary, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = friend.profile.auraScore.grouped(),
                    color = tierColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Text("#${friend.rank}", color = AuraPalette.TextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.width(14.dp))
            PlusButton(onClick = onReport)
        }
    }
}

// ── Petits boutons thémés ────────────────────────────────────────

@Composable
private fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuraPalette.Primary,
            contentColor = AuraPalette.OnPrimary,
        ),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, AuraPalette.CardBorder), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = AuraPalette.TextSecondary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
private fun PlusButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(AuraPalette.SurfaceElevated)
            .border(BorderStroke(1.dp, AuraPalette.CardBorder), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("+", color = AuraPalette.TextSecondary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}
