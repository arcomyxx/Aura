package com.dubois.aura.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.RankedUser
import com.dubois.aura.ui.components.AuraCard
import com.dubois.aura.ui.components.InitialsAvatar
import com.dubois.aura.ui.components.SectionLabel
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.color
import com.dubois.aura.util.grouped

@Composable
fun LeaderboardTab(
    leaderboard: List<RankedUser>,
    modifier: Modifier = Modifier,
) {
    val podium = leaderboard.take(3)
    val rest = leaderboard.drop(3)

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            SectionLabel("👑 Classement des amis")
            Spacer(Modifier.height(16.dp))
        }
        if (podium.isNotEmpty()) {
            item {
                PodiumBlock(podium)
                Spacer(Modifier.height(16.dp))
            }
        }
        items(rest, key = { it.profile.uid }) { user ->
            LeaderboardRow(user)
            Spacer(Modifier.height(10.dp))
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun PodiumBlock(top: List<RankedUser>) {
    // Disposition : 2e à gauche, 1er au centre, 3e à droite
    val slots: List<RankedUser?> = when (top.size) {
        1 -> listOf(null, top[0], null)
        2 -> listOf(top[1], top[0], null)
        else -> listOf(top[1], top[0], top[2])
    }
    val blockHeights = listOf(90, 130, 70)   // hauteurs 2e / 1er / 3e
    val ranks = listOf(2, 1, 3)
    val avatarSizes = listOf(48, 60, 44)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        slots.forEachIndexed { i, user ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                // Trophée au-dessus du 1er (ou spacer pour aligner)
                if (ranks[i] == 1) {
                    if (user != null) Text("🏆", fontSize = 22.sp)
                    else Spacer(Modifier.height(22.dp))
                    Spacer(Modifier.height(2.dp))
                } else {
                    Spacer(Modifier.height(24.dp))
                }

                if (user != null) {
                    // Avatar avec anneau tier
                    InitialsAvatar(
                        name = user.profile.displayName,
                        ringColor = user.tier.color,
                        size = avatarSizes[i],
                        auraScore = user.profile.auraScore,
                    )
                    Spacer(Modifier.height(6.dp))

                    // Prénom
                    Text(
                        text = user.profile.displayName.split(" ").first(),
                        color = if (user.isMe) AuraPalette.Primary else AuraPalette.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))

                    // Badge score
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AuraPalette.SurfaceElevated)
                            .border(1.dp, AuraPalette.CardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = user.profile.auraScore.grouped(),
                            color = user.tier.color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                } else {
                    // Placeholder place vide
                    Box(
                        modifier = Modifier
                            .size(avatarSizes[i].dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(AuraPalette.SurfaceElevated)
                            .border(1.5.dp, AuraPalette.CardBorder, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("?", color = AuraPalette.TextMuted, fontWeight = FontWeight.Bold, fontSize = (avatarSizes[i] * 0.4f).sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("—", color = AuraPalette.TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AuraPalette.SurfaceElevated)
                            .border(1.dp, AuraPalette.CardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("—", color = AuraPalette.TextMuted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))

                // Marche du podium
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(blockHeights[i].dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    AuraPalette.SurfaceElevated,
                                    AuraPalette.Surface.copy(alpha = 0.5f),
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent,
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = ranks[i].toString(),
                        color = AuraPalette.TextMuted.copy(alpha = 0.35f),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                    )
                }
            }
        }
    }
}

private fun medalFor(rank: Int): String? = when (rank) {
    1 -> "🥇" // 🥇
    2 -> "🥈" // 🥈
    3 -> "🥉" // 🥉
    else -> null
}

@Composable
private fun LeaderboardRow(user: RankedUser) {
    val tierColor = user.tier.color
    val medal = medalFor(user.rank)
    AuraCard(Modifier.fillMaxWidth(), highlighted = user.isMe) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) {
                if (medal != null) {
                    Text(medal, fontSize = 20.sp)
                } else {
                    Text("#${user.rank}", color = AuraPalette.TextMuted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(10.dp))
            InitialsAvatar(
                name = user.profile.displayName,
                ringColor = tierColor,
                size = 42,
                auraScore = user.profile.auraScore,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.profile.displayName.ifBlank { "(sans nom)" },
                        color = if (user.isMe) AuraPalette.Primary else AuraPalette.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    if (user.isMe) {
                        Spacer(Modifier.width(6.dp))
                        Text("(toi)", color = AuraPalette.TextMuted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(user.tier.displayName, color = AuraPalette.TextSecondary, fontSize = 12.sp)
            }
            Text(
                text = user.profile.auraScore.grouped(),
                color = tierColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}
