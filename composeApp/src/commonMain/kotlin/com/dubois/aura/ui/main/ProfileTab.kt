package com.dubois.aura.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dubois.aura.model.AuraEvent
import com.dubois.aura.model.AuraTier
import com.dubois.aura.model.RankedUser
import com.dubois.aura.ui.components.AuraCard
import com.dubois.aura.ui.components.AuraGauge
import com.dubois.aura.ui.components.InitialsAvatar
import com.dubois.aura.ui.components.SectionLabel
import com.dubois.aura.ui.components.clickableNoRipple
import com.dubois.aura.ui.main.GradientButton
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.color
import com.dubois.aura.ui.theme.toHex
import com.dubois.aura.util.LocalShareProfile
import com.dubois.aura.util.relativeTime
import com.dubois.aura.util.signed
import kotlin.time.Clock

@Composable
fun ProfileTab(
    me: RankedUser?,
    events: List<AuraEvent>,
    weeklyDelta: Int,
    modifier: Modifier = Modifier,
) {
    val tier = me?.tier
    val tierColor = tier?.color ?: AuraPalette.TierBase
    val now = Clock.System.now().toEpochMilliseconds()
    val shareProfile = LocalShareProfile.current
    var showAuraLevels by remember { mutableStateOf(false) }

    if (showAuraLevels) {
        AuraLevelsDialog(onDismiss = { showAuraLevels = false })
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            AuraGauge(score = me?.profile?.auraScore ?: 0, tierColor = tierColor)
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tier?.displayName ?: "—",
                    color = tierColor,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(AuraPalette.SurfaceElevated)
                        .border(1.dp, AuraPalette.CardBorder, CircleShape)
                        .clickableNoRipple { showAuraLevels = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("?", color = AuraPalette.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Rang #${me?.rank ?: "—"}",
                    color = AuraPalette.TextSecondary,
                    fontSize = 14.sp,
                )
                if (weeklyDelta != 0) {
                    Spacer(Modifier.width(12.dp))
                    val positive = weeklyDelta >= 0
                    Text(
                        text = "${if (positive) "↗" else "↘"} ${weeklyDelta.signed()} cette semaine",
                        color = if (positive) AuraPalette.Positive else AuraPalette.Negative,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (me != null) {
                Spacer(Modifier.height(20.dp))
                GradientButton(
                    text = "✨ Partager mon aura",
                    onClick = {
                        shareProfile(
                            me.profile.displayName,
                            me.tier.displayName,
                            me.profile.auraScore,
                            me.rank,
                            me.tier.color.toHex(),
                        )
                    },
                )
            }
            Spacer(Modifier.height(28.dp))
            SectionLabel("Événements récents", Modifier.fillMaxWidth())
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
                EventRow(event, now)
                Spacer(Modifier.height(12.dp))
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// Score représentatif pour chaque tier (milieu du palier)
private val TIER_DEMO_SCORES = listOf(100, 300, 500, 700, 900, 1050)

@Composable
private fun AuraLevelsDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AuraPalette.Surface)
                    .border(1.dp, AuraPalette.CardBorder, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(vertical = 24.dp, horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Niveaux d'aura",
                    color = AuraPalette.TextPrimary,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Plus ton aura est élevée, plus l'animation s'intensifie.",
                    color = AuraPalette.TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(20.dp))
                AuraTier.entries.forEachIndexed { idx, tier ->
                    val demoScore = TIER_DEMO_SCORES[idx]
                    val tierColor = tier.color
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AuraGauge(
                            score = demoScore,
                            tierColor = tierColor,
                            diameter = 72,
                            padded = false,
                        )
                        Column {
                            Text(
                                tier.displayName,
                                color = tierColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                            Text(
                                if (idx == AuraTier.entries.lastIndex) "≥ ${tier.minScore} pts"
                                else "${tier.minScore} – ${AuraTier.entries[idx + 1].minScore - 1} pts",
                                color = AuraPalette.TextMuted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    if (idx < AuraTier.entries.lastIndex) {
                        Box(
                            Modifier.fillMaxWidth().height(1.dp)
                                .background(AuraPalette.CardBorder)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: AuraEvent, now: Long) {
    AuraCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
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
                    text = event.description.ifBlank { event.label },
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
    }
}
