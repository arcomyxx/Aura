package com.dubois.aura.ui.main

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.Battle
import com.dubois.aura.model.BattleRules
import com.dubois.aura.model.RankedUser
import com.dubois.aura.ui.components.AuraCard
import com.dubois.aura.ui.components.InitialsAvatar
import com.dubois.aura.ui.components.SectionLabel
import com.dubois.aura.ui.components.clickableNoRipple
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.colorFromHex
import com.dubois.aura.util.grouped
import com.dubois.aura.util.relativeTime
import com.dubois.aura.util.signed

/**
 * Onglet Battle : lancement d'une battle, battles en direct des amis et
 * historique des battles hébergées ou soutenues.
 */
@Composable
fun BattleTab(
    myLiveBattle: Battle?,
    friendLiveBattles: List<Battle>,
    history: List<Battle>,
    usersByUid: Map<String, RankedUser>,
    myUid: String?,
    now: Long,
    launching: Boolean,
    onLaunch: () -> Unit,
    onOpen: (Battle) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item {
            LaunchCard(
                myLiveBattle = myLiveBattle,
                now = now,
                launching = launching,
                onLaunch = onLaunch,
                onResume = { myLiveBattle?.let(onOpen) },
            )
            Spacer(Modifier.height(26.dp))
            SectionLabel("En direct")
            Spacer(Modifier.height(12.dp))
        }

        if (friendLiveBattles.isEmpty()) {
            item {
                Text(
                    text = "Aucun ami en battle pour l'instant.",
                    color = AuraPalette.TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(friendLiveBattles, key = { "live-" + it.id }) { battle ->
                LiveBattleRow(battle, usersByUid[battle.hostUid], now, onClick = { onOpen(battle) })
                Spacer(Modifier.height(12.dp))
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
            SectionLabel("Historique")
            Spacer(Modifier.height(12.dp))
        }

        val past = history.filterNot { it.isLive(now) }
        if (past.isEmpty()) {
            item {
                Text(
                    text = "Tes battles terminées apparaîtront ici.",
                    color = AuraPalette.TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(past, key = { "hist-" + it.id }) { battle ->
                HistoryRow(battle, usersByUid[battle.hostUid], battle.hostUid == myUid, now, onClick = { onOpen(battle) })
                Spacer(Modifier.height(12.dp))
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun LaunchCard(
    myLiveBattle: Battle?,
    now: Long,
    launching: Boolean,
    onLaunch: () -> Unit,
    onResume: () -> Unit,
) {
    AuraCard(Modifier.fillMaxWidth(), highlighted = true) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚔️", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Battle d'aura",
                color = AuraPalette.TextPrimary,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${BattleRules.DURATION_MS / 1000} secondes sur scène. Tes amis tapent pour faire " +
                    "exploser ton score, et 10 % du score final s'ajoute à ton aura.",
                color = AuraPalette.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(18.dp))
            val live = myLiveBattle?.takeIf { it.isLive(now) }
            GradientButton(
                text = when {
                    live != null -> "Reprendre ma battle · ${live.remainingMs(now) / 1000}s"
                    launching -> "Lancement…"
                    else -> "Lancer une battle"
                },
                onClick = if (live != null) onResume else onLaunch,
                enabled = live != null || !launching,
            )
        }
    }
}

/** Bouton plein au dégradé d'accent. */
@Composable
internal fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(AuraPalette.PrimaryGradientStart, AuraPalette.PrimaryGradientEnd)
                )
            )
            .alpha(if (enabled) 1f else 0.6f)
            .clickableNoRipple { if (enabled) onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = AuraPalette.OnPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

/** Pastille « LIVE » rouge clignotante. */
@Composable
internal fun LiveBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AuraPalette.Negative.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(7.dp).alpha(dotAlpha).clip(CircleShape).background(AuraPalette.Negative)
        )
        Spacer(Modifier.width(5.dp))
        Text("LIVE", color = AuraPalette.Negative, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun LiveBattleRow(battle: Battle, host: RankedUser?, now: Long, onClick: () -> Unit) {
    val accent = colorFromHex(battle.hostThemeColor) ?: AuraPalette.Primary
    AuraCard(Modifier.fillMaxWidth().clickableNoRipple(onClick), accent = accent) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(
                name = battle.hostName,
                ringColor = accent,
                size = 44,
                photoBase64 = host?.profile?.photoUrl ?: "",
                auraScore = host?.profile?.auraScore,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = battle.hostName.ifBlank { "Anonyme" },
                        color = AuraPalette.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    LiveBadge()
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${battle.score.grouped()} aura · encore ${battle.remainingMs(now) / 1000}s",
                    color = AuraPalette.TextSecondary,
                    fontSize = 13.sp,
                )
            }
            Text("Rejoindre →", color = accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun HistoryRow(battle: Battle, host: RankedUser?, isMine: Boolean, now: Long, onClick: () -> Unit) {
    AuraCard(Modifier.fillMaxWidth().clickableNoRipple(onClick)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(
                name = battle.hostName,
                ringColor = colorFromHex(battle.hostThemeColor) ?: AuraPalette.TierBase,
                size = 40,
                fontSize = 13,
                photoBase64 = host?.profile?.photoUrl ?: "",
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isMine) "Ma battle" else battle.hostName.ifBlank { "Anonyme" },
                        color = AuraPalette.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(relativeTime(battle.startedAt, now), color = AuraPalette.TextMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${battle.supporterUids.size} supporters · combo max x${battle.maxCombo} · ${battle.taps.grouped()} taps",
                    color = AuraPalette.TextSecondary,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                Text(battle.score.grouped(), color = AuraPalette.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (isMine && battle.auraAwarded > 0) {
                    Text(
                        text = "${battle.auraAwarded.signed()} aura",
                        color = AuraPalette.Positive,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
