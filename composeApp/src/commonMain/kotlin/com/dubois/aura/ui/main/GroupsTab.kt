package com.dubois.aura.ui.main

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.EventCatalog
import com.dubois.aura.model.EventType
import com.dubois.aura.model.Group
import com.dubois.aura.model.GroupInvite
import com.dubois.aura.model.GroupMember
import com.dubois.aura.model.RankedUser
import com.dubois.aura.model.UserProfile
import com.dubois.aura.ui.components.AuraCard
import com.dubois.aura.ui.components.AuraIcon
import com.dubois.aura.ui.components.IconCircleButton
import com.dubois.aura.ui.components.AuraGauge
import com.dubois.aura.ui.components.InitialsAvatar
import com.dubois.aura.ui.components.SectionLabel
import com.dubois.aura.ui.components.blockTouchesBehind
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.color
import com.dubois.aura.util.grouped

// ── Onglet liste des groupes ──────────────────────────────────────────────────

@Composable
fun GroupsTab(
    groups: List<Group>,
    groupInvites: List<GroupInvite>,
    myUid: String?,
    leaderboard: List<com.dubois.aura.model.RankedUser>,
    onCreateGroup: (String) -> Unit,
    onAcceptInvite: (GroupInvite) -> Unit,
    onDeclineInvite: (GroupInvite) -> Unit,
    onOpenGroup: (Group) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreate by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("🏆 Mes groupes", Modifier.weight(1f))
                IconCircleButton(
                    icon = AuraIcon.Plus,
                    onClick = { showCreate = !showCreate },
                    size = 36,
                    iconSize = 14,
                )
            }
            Spacer(Modifier.height(10.dp))

            if (showCreate) {
                AuraCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "Nouveau groupe",
                            color = AuraPalette.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            placeholder = { Text("Nom du groupe", color = AuraPalette.TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraPalette.Primary,
                                unfocusedBorderColor = AuraPalette.CardBorder,
                                focusedTextColor = AuraPalette.TextPrimary,
                                unfocusedTextColor = AuraPalette.TextPrimary,
                                cursorColor = AuraPalette.Primary,
                            ),
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (newGroupName.isNotBlank()) {
                                    onCreateGroup(newGroupName.trim())
                                    newGroupName = ""
                                    showCreate = false
                                }
                            },
                            enabled = newGroupName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = AuraPalette.Primary),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Créer", color = AuraPalette.Background, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }

        if (groupInvites.isNotEmpty()) {
            item {
                SectionLabel("Invitations")
                Spacer(Modifier.height(8.dp))
            }
            items(groupInvites, key = { it.id }) { invite ->
                GroupInviteRow(
                    invite = invite,
                    onAccept = { onAcceptInvite(invite) },
                    onDecline = { onDeclineInvite(invite) },
                )
                Spacer(Modifier.height(8.dp))
            }
            item { Spacer(Modifier.height(6.dp)) }
        }

        if (groups.isEmpty() && groupInvites.isEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Pas encore de groupe. Créez-en un et invitez vos amis !",
                    color = AuraPalette.TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        } else if (groups.isNotEmpty()) {
            item {
                if (groupInvites.isNotEmpty()) {
                    SectionLabel("Mes groupes")
                    Spacer(Modifier.height(8.dp))
                }
            }
            items(groups, key = { it.id }) { group ->
                GroupRow(group = group, myUid = myUid, onClick = { onOpenGroup(group) })
                Spacer(Modifier.height(8.dp))
            }
        }

        // Classement global en section secondaire
        if (leaderboard.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AuraPalette.CardBorder)
                )
                Spacer(Modifier.height(16.dp))
                SectionLabel("🌍 Classement général", modifier = Modifier)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Score global tous groupes confondus",
                    color = AuraPalette.TextMuted,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(12.dp))
                val podium = leaderboard.take(3)
                if (podium.isNotEmpty()) {
                    GlobalPodiumBlock(podium)
                    Spacer(Modifier.height(12.dp))
                }
            }
            items(leaderboard.drop(3), key = { "global_${it.profile.uid}" }) { user ->
                GlobalLeaderboardRow(user)
                Spacer(Modifier.height(8.dp))
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun GroupInviteRow(
    invite: GroupInvite,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AuraCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(invite.groupName, color = AuraPalette.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Invitation de ${invite.fromName}",
                    color = AuraPalette.TextMuted,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "✓",
                color = AuraPalette.Positive,
                fontSize = 18.sp,
                modifier = Modifier.clickable { onAccept() },
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "✕",
                color = AuraPalette.Negative,
                fontSize = 16.sp,
                modifier = Modifier.clickable { onDecline() },
            )
        }
    }
}

@Composable
private fun GroupRow(
    group: Group,
    myUid: String?,
    onClick: () -> Unit,
) {
    AuraCard(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(AuraPalette.Primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("👥", fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(group.name, color = AuraPalette.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                val role = if (group.createdBy == myUid) "Créateur · " else ""
                Text(
                    "${role}${group.memberUids.size} membre${if (group.memberUids.size > 1) "s" else ""}",
                    color = AuraPalette.TextMuted,
                    fontSize = 12.sp,
                )
            }
            Text("›", color = AuraPalette.TextMuted, fontSize = 20.sp)
        }
    }
}

// ── Écran détail d'un groupe ──────────────────────────────────────────────────

@Composable
fun GroupDetailScreen(
    group: Group,
    leaderboard: List<GroupMember>,
    loading: Boolean,
    friends: List<RankedUser>,
    myUid: String?,
    onBack: () -> Unit,
    onInvite: (UserProfile) -> Unit,
    onReport: (GroupMember, EventType, String) -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
) {
    var showInvite by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<GroupMember?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(AuraPalette.Background)
            .blockTouchesBehind()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(horizontal = 20.dp)
        ) {
            // En-tête
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircleButton(icon = AuraIcon.Back, onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        group.name,
                        color = AuraPalette.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    )
                    Text(
                        "${group.memberUids.size} membre${if (group.memberUids.size > 1) "s" else ""} · scores propres au groupe",
                        color = AuraPalette.TextMuted,
                        fontSize = 12.sp,
                    )
                }
                Box {
                    Text(
                        "⋯",
                        color = AuraPalette.TextMuted,
                        fontSize = 22.sp,
                        modifier = Modifier.clickable { showOptions = true },
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = showOptions,
                        onDismissRequest = { showOptions = false },
                        containerColor = AuraPalette.SurfaceElevated,
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Inviter un ami", color = AuraPalette.TextPrimary, fontSize = 14.sp) },
                            onClick = { showOptions = false; showInvite = true },
                        )
                        if (group.createdBy == myUid) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Supprimer le groupe", color = AuraPalette.Negative, fontSize = 14.sp) },
                                onClick = { showOptions = false; onDelete() },
                            )
                        } else {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Quitter le groupe", color = AuraPalette.Negative, fontSize = 14.sp) },
                                onClick = { showOptions = false; onLeave() },
                            )
                        }
                    }
                }
            }

            // Panel invitation d'un ami
            if (showInvite) {
                val invitableFriends = friends.filter { it.profile.uid !in group.memberUids }
                AuraCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Inviter un ami",
                                color = AuraPalette.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Text("✕", color = AuraPalette.TextMuted, fontSize = 16.sp, modifier = Modifier.clickable { showInvite = false })
                        }
                        Spacer(Modifier.height(10.dp))
                        if (invitableFriends.isEmpty()) {
                            Text("Tous vos amis sont déjà membres.", color = AuraPalette.TextMuted, fontSize = 13.sp)
                        } else {
                            invitableFriends.forEach { friend ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onInvite(friend.profile); showInvite = false }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    InitialsAvatar(
                                        name = friend.profile.displayName,
                                        ringColor = friend.tier.color,
                                        size = 36,
                                        photoBase64 = friend.profile.photoUrl,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        friend.profile.displayName.ifBlank { friend.profile.email },
                                        color = AuraPalette.TextPrimary,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Classement du groupe
            SectionLabel("🏆 Classement du groupe")
            Spacer(Modifier.height(10.dp))

            if (loading) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = AuraPalette.Primary,
                    modifier = Modifier.size(28.dp),
                )
            } else if (leaderboard.isEmpty()) {
                Text("Aucun membre pour l'instant.", color = AuraPalette.TextMuted, fontSize = 13.sp)
            } else {
                val podium = leaderboard.take(3)
                val rest = leaderboard.drop(3)
                LazyColumn {
                    if (podium.isNotEmpty()) {
                        item {
                            GroupPodiumBlock(podium, myUid)
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                    items(rest.size) { index ->
                        val member = rest[index]
                        GroupMemberRow(
                            member = member,
                            rank = podium.size + index + 1,
                            isMe = member.uid == myUid,
                            canReport = member.uid != myUid,
                            onReport = { reportTarget = member },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }

        // Sheet de signalement dans le groupe
        reportTarget?.let { target ->
            GroupReportOverlay(
                target = target,
                onDismiss = { reportTarget = null },
                onSubmit = { type, description ->
                    onReport(target, type, description)
                    reportTarget = null
                },
            )
        }
    }
}

@Composable
private fun GlobalPodiumBlock(top: List<com.dubois.aura.model.RankedUser>) {
    val order = when (top.size) {
        1 -> listOf(top[0])
        2 -> listOf(top[1], top[0])
        else -> listOf(top[1], top[0], top[2])
    }
    val heights = when (top.size) { 1 -> listOf(60); 2 -> listOf(44, 60); else -> listOf(44, 60, 36) }
    val medals = listOf("🥈", "🥇", "🥉")
    val podiumColors = listOf(
        androidx.compose.ui.graphics.Color(0xFFB0BEC5),
        androidx.compose.ui.graphics.Color(0xFFFFD700),
        androidx.compose.ui.graphics.Color(0xFFCD7F32),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        order.forEachIndexed { i, user ->
            val tierColor = user.tier.color
            val medalColor = podiumColors[i]
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AuraGauge(score = user.profile.auraScore, tierColor = tierColor, diameter = if (i == 1) 86 else 70, name = user.profile.displayName, photoBase64 = user.profile.photoUrl)
                Text(user.profile.displayName.split(" ").first(), color = if (user.isMe) AuraPalette.Primary else AuraPalette.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxWidth().height(heights[i].dp).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(medalColor.copy(alpha = 0.55f), medalColor.copy(alpha = 0.12f)))),
                    contentAlignment = Alignment.TopCenter,
                ) { Text(medals[i], fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp)) }
            }
        }
    }
}

@Composable
private fun GlobalLeaderboardRow(user: com.dubois.aura.model.RankedUser) {
    val tierColor = user.tier.color
    val medal = when (user.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }
    AuraCard(Modifier.fillMaxWidth(), highlighted = user.isMe) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                if (medal != null) Text(medal, fontSize = 16.sp)
                else Text("#${user.rank}", color = AuraPalette.TextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            AuraGauge(score = user.profile.auraScore, tierColor = tierColor, diameter = 60, padded = false, name = user.profile.displayName, photoBase64 = user.profile.photoUrl)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = user.profile.displayName.ifBlank { "(sans nom)" } + if (user.isMe) " (toi)" else "",
                    color = if (user.isMe) AuraPalette.Primary else AuraPalette.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Text(user.tier.displayName, color = AuraPalette.TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun GroupPodiumBlock(top: List<GroupMember>, myUid: String?) {
    val order = when (top.size) {
        1 -> listOf(top[0])
        2 -> listOf(top[1], top[0])
        else -> listOf(top[1], top[0], top[2])
    }
    val heights = when (top.size) {
        1 -> listOf(80)
        2 -> listOf(60, 80)
        else -> listOf(60, 80, 50)
    }
    val medals = listOf("🥈", "🥇", "🥉")
    // ordre d'affichage [2e, 1er, 3e] → couleurs de podium correspondantes
    val podiumColors = listOf(
        androidx.compose.ui.graphics.Color(0xFFB0BEC5), // argent
        androidx.compose.ui.graphics.Color(0xFFFFD700), // or
        androidx.compose.ui.graphics.Color(0xFFCD7F32), // bronze
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        order.forEachIndexed { i, member ->
            val tierColor = member.tier.color
            val medalColor = podiumColors[i]
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AuraGauge(
                    score = member.groupAuraScore,
                    tierColor = tierColor,
                    diameter = if (i == 1) 86 else 70,
                    name = member.displayName,
                    photoBase64 = member.photoUrl,
                )
                Text(
                    text = member.displayName.split(" ").first(),
                    color = if (member.uid == myUid) AuraPalette.Primary else AuraPalette.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heights[i].dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(medalColor.copy(alpha = 0.55f), medalColor.copy(alpha = 0.12f))
                            )
                        ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(medals[i], fontSize = 20.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun GroupMemberRow(
    member: GroupMember,
    rank: Int,
    isMe: Boolean,
    canReport: Boolean,
    onReport: () -> Unit,
) {
    val tierColor = member.tier.color
    val medal = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }
    AuraCard(Modifier.fillMaxWidth(), highlighted = isMe) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                if (medal != null) Text(medal, fontSize = 18.sp)
                else Text("#$rank", color = AuraPalette.TextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.width(10.dp))
            AuraGauge(
                score = member.groupAuraScore,
                tierColor = tierColor,
                diameter = 60,
                padded = false,
                name = member.displayName,
                photoBase64 = member.photoUrl,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.displayName.ifBlank { "(sans nom)" },
                        color = if (isMe) AuraPalette.Primary else AuraPalette.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    if (isMe) {
                        Spacer(Modifier.width(5.dp))
                        Text("(toi)", color = AuraPalette.TextMuted, fontSize = 11.sp)
                    }
                }
                Text(member.tier.displayName, color = AuraPalette.TextSecondary, fontSize = 11.sp)
            }
            if (canReport) {
                Text(
                    "+ Signaler",
                    color = AuraPalette.Primary,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { onReport() },
                )
            }
        }
    }
}

// ── Overlay de signalement dans un groupe ────────────────────────────────────

@Composable
private fun GroupReportOverlay(
    target: GroupMember,
    onDismiss: () -> Unit,
    onSubmit: (EventType, String) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AuraPalette.Background.copy(alpha = 0.85f))
            .blockTouchesBehind()
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        AuraCard(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clickable { /* absorb */ },
        ) {
            GroupReportContent(
                targetName = target.displayName,
                onDismiss = onDismiss,
                onSubmit = onSubmit,
            )
        }
    }
}

@Composable
private fun GroupReportContent(
    targetName: String,
    onDismiss: () -> Unit,
    onSubmit: (EventType, String) -> Unit,
) {
    var selectedType by remember { mutableStateOf<EventType?>(null) }
    var description by remember { mutableStateOf("") }
    val types = EventCatalog.all

    Column(Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Signaler pour $targetName",
                color = AuraPalette.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f),
            )
            Text("✕", color = AuraPalette.TextMuted, fontSize = 16.sp, modifier = Modifier.clickable { onDismiss() })
        }
        Spacer(Modifier.height(4.dp))
        Text("Score propre au groupe", color = AuraPalette.TextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(14.dp))

        // Grille de types d'événements (5 colonnes)
        val rows = types.chunked(5)
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { type ->
                    val selected = type == selectedType
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(3.dp)
                            .background(
                                if (selected) AuraPalette.Primary.copy(alpha = 0.2f)
                                else AuraPalette.SurfaceElevated,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable { selectedType = type }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(type.emoji, fontSize = 18.sp)
                            Text(
                                if (type.delta >= 0) "+${type.delta}" else "${type.delta}",
                                color = if (type.delta >= 0) AuraPalette.Positive else AuraPalette.Negative,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                // Rembourrage si la rangée est incomplète
                for (i in row.size until 5) { Box(Modifier.weight(1f)) {} }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (selectedType != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Description (optionnel)", color = AuraPalette.TextMuted, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuraPalette.Primary,
                    unfocusedBorderColor = AuraPalette.CardBorder,
                    focusedTextColor = AuraPalette.TextPrimary,
                    unfocusedTextColor = AuraPalette.TextPrimary,
                    cursorColor = AuraPalette.Primary,
                ),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { selectedType?.let { onSubmit(it, description) } },
                colors = ButtonDefaults.buttonColors(containerColor = AuraPalette.Primary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Valider", color = AuraPalette.Background, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
