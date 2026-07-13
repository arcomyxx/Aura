package com.dubois.aura.ui.main

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            SectionLabel("👑 Classement des amis")
            Spacer(Modifier.height(14.dp))
        }
        items(leaderboard, key = { it.profile.uid }) { user ->
            LeaderboardRow(user)
            Spacer(Modifier.height(10.dp))
        }
        item { Spacer(Modifier.height(24.dp)) }
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
            InitialsAvatar(name = user.profile.displayName, ringColor = tierColor, size = 42)
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
