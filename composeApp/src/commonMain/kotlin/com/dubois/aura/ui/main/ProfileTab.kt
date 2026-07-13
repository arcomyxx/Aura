package com.dubois.aura.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.color
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

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            AuraGauge(score = me?.profile?.auraScore ?: 0, tierColor = tierColor)
            Spacer(Modifier.height(20.dp))
            Text(
                text = tier?.displayName ?: "—",
                color = tierColor,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            )
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
