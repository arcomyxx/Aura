package com.dubois.aura.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.dubois.aura.ui.components.blockTouchesBehind
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.Battle
import com.dubois.aura.model.BattleAction
import com.dubois.aura.model.BattleRules
import com.dubois.aura.model.BattleSupporter
import com.dubois.aura.model.RankedUser
import com.dubois.aura.ui.components.AuraCard
import com.dubois.aura.ui.components.InitialsAvatar
import com.dubois.aura.ui.components.SectionLabel
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.colorFromHex
import com.dubois.aura.util.grouped
import com.dubois.aura.util.signed
import kotlin.random.Random

/** Popup « +X » flottant au-dessus du bouton de hype. */
private data class TapPopup(val id: Long, val text: String, val dx: Int)

/**
 * Écran d'une battle d'aura, superposé aux onglets. En direct : score temps
 * réel, chrono, jauge de hype et — pour les supporters — boutons de soutien
 * avec combo. Une fois le chrono écoulé : récapitulatif de la battle.
 */
@Composable
fun BattleScreen(
    battle: Battle,
    host: RankedUser?,
    supporters: List<BattleSupporter>,
    myUid: String?,
    now: Long,
    pendingPoints: Int,
    combo: Int,
    myBestCombo: Int,
    readyAt: Map<BattleAction, Long>,
    hype: Float,
    onTap: (BattleAction) -> Int?,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onShareBattle: () -> Unit = {},
    onInvite: () -> Unit = {},
) {
    val accent = colorFromHex(battle.hostThemeColor) ?: AuraPalette.Primary
    val isHost = battle.hostUid == myUid
    val live = battle.isLive(now)
    val remaining = battle.remainingMs(now)
    var confirmCancel by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blockTouchesBehind()
            .background(AuraPalette.Background)
            .background(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = if (live) 0.28f else 0.14f), AuraPalette.Background),
                    center = Offset(0.5f * 1000f, 0f),
                    radius = 1100f,
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // En-tête : retour, hôte, statut
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    SectionLabel("Battle d'aura")
                    Text(
                        text = if (isHost) "Ta battle" else battle.hostName.ifBlank { "Anonyme" },
                        color = AuraPalette.TextPrimary,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                    )
                }
                InitialsAvatar(
                    name = battle.hostName,
                    ringColor = accent,
                    size = 48,
                    photoBase64 = host?.profile?.photoUrl ?: "",
                    auraScore = host?.profile?.auraScore,
                )
            }
            Spacer(Modifier.height(18.dp))

            if (live) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveBadge()
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = formatCountdown(remaining),
                        color = if (BattleRules.isFinalRush(remaining)) AuraPalette.Negative else AuraPalette.TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
                Spacer(Modifier.height(6.dp))
                TimerBar(fraction = remaining / BattleRules.DURATION_MS.toFloat(), color = accent)
                if (isHost) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "✕ Annuler la battle",
                        color = AuraPalette.Negative,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, AuraPalette.Negative.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
                            .clickable { confirmCancel = true }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }

            LiveScore(score = battle.score + pendingPoints, color = accent)

            if (live) {
                if (BattleRules.isFinalRush(remaining)) {
                    Text(
                        text = "⚡ RUSH FINAL · POINTS x2 ⚡",
                        color = AuraPalette.TierSupreme,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                HypeMeter(hype = hype, color = accent)
                Spacer(Modifier.height(16.dp))
                if (isHost) {
                    HostStage(supporters, Modifier.weight(1f))
                } else {
                    SupporterControls(
                        combo = combo,
                        readyAt = readyAt,
                        now = now,
                        remaining = remaining,
                        accent = accent,
                        onTap = onTap,
                        onInvite = onInvite,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                BattleResults(
                    battle = battle,
                    supporters = supporters,
                    isHost = isHost,
                    myUid = myUid,
                    myBestCombo = myBestCombo,
                    onShare = onShareBattle,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // Confirmation d'annulation (hôte)
    if (confirmCancel && live) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            containerColor = AuraPalette.SurfaceElevated,
            title = { Text("Annuler la battle ?", color = AuraPalette.TextPrimary) },
            text = {
                Text(
                    "La battle s'arrête pour tout le monde et aucune aura ne sera gagnée.",
                    color = AuraPalette.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    onCancel()
                }) {
                    Text("Annuler la battle", color = AuraPalette.Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) {
                    Text("Continuer", color = AuraPalette.TextSecondary)
                }
            },
        )
    }
}

private fun formatCountdown(ms: Long): String {
    val totalSec = (ms + 999) / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "$min:${sec.toString().padStart(2, '0')}"
}

@Composable
private fun TimerBar(fraction: Float, color: Color) {
    Box(
        Modifier.fillMaxWidth(0.6f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(AuraPalette.CardBorder)
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(color))
    }
}

/** Score géant qui défile vers sa valeur et « pulse » à chaque hausse. */
@Composable
private fun LiveScore(score: Int, color: Color) {
    val animated by animateIntAsState(score, animationSpec = tween(350))
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(score) {
        if (score > 0) {
            pulse.snapTo(1.08f)
            pulse.animateTo(1f, tween(220))
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 14.dp)) {
        Text(
            text = animated.grouped(),
            color = color,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp,
            modifier = Modifier.scale(pulse.value),
        )
        Text("AURA", color = AuraPalette.TextMuted, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Jauge de hype : nombre de taps/s de toute la salle. */
@Composable
private fun HypeMeter(hype: Float, color: Color) {
    val level by animateFloatAsState((hype / HYPE_MAX_TAPS_PER_SEC).coerceIn(0f, 1f), tween(250))
    val label = when {
        level > 0.8f -> "🔥🔥🔥 LA SALLE EXPLOSE"
        level > 0.45f -> "🔥🔥 ÇA CHAUFFE"
        level > 0.1f -> "🔥 ÇA MONTE"
        else -> "Calme plat…"
    }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text("HYPE", color = AuraPalette.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            Text(label, color = AuraPalette.TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(AuraPalette.CardBorder)) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(level)
                    .background(Brush.horizontalGradient(listOf(color, AuraPalette.TierSupreme, AuraPalette.Negative)))
            )
        }
    }
}

/** Vue de l'hôte pendant sa battle : il ne tape pas, il regarde ses supporters. */
@Composable
private fun HostStage(supporters: List<BattleSupporter>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = "Tu es sur scène 🎤 Montre ton aura, tes amis tapent pour toi !",
            color = AuraPalette.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        SupportersList(supporters, myUid = null, Modifier.weight(1f))
    }
}

@Composable
private fun SupporterControls(
    combo: Int,
    readyAt: Map<BattleAction, Long>,
    now: Long,
    remaining: Long,
    accent: Color,
    onTap: (BattleAction) -> Int?,
    onInvite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val popups = remember { mutableStateListOf<TapPopup>() }

    fun handle(action: BattleAction) {
        val points = onTap(action) ?: return
        val tierUp = BattleRules.comboMultiplier(combo + 1) > BattleRules.comboMultiplier(combo)
        haptics.performHapticFeedback(
            if (tierUp || action != BattleAction.HYPE) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
        )
        popups += TapPopup(Random.nextLong(), "+$points", Random.nextInt(-70, 70))
        if (popups.size > 14) popups.removeAt(0)
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        ComboDisplay(combo, remaining)
        Spacer(Modifier.weight(1f))

        Box(contentAlignment = Alignment.Center) {
            HypeButton(accent = accent, onClick = { handle(BattleAction.HYPE) })
            popups.forEach { popup ->
                androidx.compose.runtime.key(popup.id) {
                    FloatingPopup(popup, onDone = { popups.remove(popup) })
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(BattleAction.CHARISME, BattleAction.AURA_MAX).forEach { action ->
                SpecialButton(
                    action = action,
                    remainingCooldown = ((readyAt[action] ?: 0L) - now).coerceAtLeast(0L),
                    points = BattleRules.pointsFor(action, combo + 1, remaining),
                    accent = accent,
                    onClick = { handle(action) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "📣 Invite tes amis",
            color = AuraPalette.TextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = androidx.compose.ui.Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onInvite)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ComboDisplay(combo: Int, remaining: Long) {
    val multiplier = BattleRules.comboMultiplier(combo) * if (BattleRules.isFinalRush(remaining)) 2 else 1
    val title = BattleRules.comboTitle(combo)
    val bump = remember { Animatable(1f) }
    LaunchedEffect(combo) {
        if (combo > 1) {
            bump.snapTo(1.18f)
            bump.animateTo(1f, tween(160))
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.height(64.dp)) {
        if (combo >= 2) {
            Text(
                text = "COMBO x$combo",
                color = if (title != null) AuraPalette.TierSupreme else AuraPalette.TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                modifier = Modifier.scale(bump.value),
            )
            Text(
                text = listOfNotNull(title, "points x$multiplier").joinToString(" · "),
                color = AuraPalette.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Text("Tape vite pour lancer un combo !", color = AuraPalette.TextMuted, fontSize = 14.sp)
        }
    }
}

/** Gros bouton rond de hype (taps illimités). */
@Composable
private fun HypeButton(accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(80))
    Box(
        modifier = Modifier
            .size(170.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.95f), accent.copy(alpha = 0.55f))))
            .border(BorderStroke(3.dp, accent.copy(alpha = 0.9f)), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(BattleAction.HYPE.emoji, fontSize = 48.sp)
            Text("HYPE", color = AuraPalette.OnPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun FloatingPopup(popup: TapPopup, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
        onDone()
    }
    Text(
        text = popup.text,
        color = AuraPalette.TextPrimary,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        modifier = Modifier
            .offset(x = popup.dx.dp, y = (-60 - 90 * progress.value).dp)
            .alpha(1f - progress.value),
    )
}

/** Bouton spécial à fort gain, bloqué pendant son cooldown. */
@Composable
private fun SpecialButton(
    action: BattleAction,
    remainingCooldown: Long,
    points: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = remainingCooldown == 0L
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(shape)
            .background(if (ready) AuraPalette.SurfaceElevated else AuraPalette.Surface)
            .border(BorderStroke(1.dp, if (ready) accent.copy(alpha = 0.7f) else AuraPalette.CardBorder), shape)
            .clickable(enabled = ready, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Remplissage de cooldown
        if (!ready) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(1f - remainingCooldown / action.cooldownMs.toFloat())
                    .background(accent.copy(alpha = 0.12f))
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(if (ready) 1f else 0.55f)) {
            Text(action.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(action.label, color = AuraPalette.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = if (ready) "+$points" else "${(remainingCooldown + 999) / 1000}s",
                    color = if (ready) accent else AuraPalette.TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun SupportersList(supporters: List<BattleSupporter>, myUid: String?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        SectionLabel("Supporters · ${supporters.size}")
        Spacer(Modifier.height(10.dp))
        if (supporters.isEmpty()) {
            Text("En attente des premiers supporters…", color = AuraPalette.TextMuted, fontSize = 14.sp)
        } else {
            LazyColumn {
                items(supporters.size) { index ->
                    SupporterRow(index + 1, supporters[index], isMe = supporters[index].uid == myUid)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SupporterRow(rank: Int, supporter: BattleSupporter, isMe: Boolean) {
    AuraCard(Modifier.fillMaxWidth(), highlighted = isMe) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#$rank" },
                color = AuraPalette.TextSecondary,
                fontSize = 16.sp,
                modifier = Modifier.width(32.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = supporter.name.ifBlank { "Anonyme" } + if (isMe) " (toi)" else "",
                    color = AuraPalette.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    text = "${supporter.taps.grouped()} taps · combo max x${supporter.bestCombo}",
                    color = AuraPalette.TextMuted,
                    fontSize = 12.sp,
                )
            }
            Text(supporter.points.signed(), color = AuraPalette.Positive, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

/** Récapitulatif d'une battle terminée. */
@Composable
private fun BattleResults(
    battle: Battle,
    supporters: List<BattleSupporter>,
    isHost: Boolean,
    myUid: String?,
    myBestCombo: Int,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mvp = supporters.firstOrNull()
    val mine = supporters.firstOrNull { it.uid == myUid }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (battle.isCancelled) "🚫 BATTLE ANNULÉE" else "🏁 BATTLE TERMINÉE",
            color = AuraPalette.TextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(8.dp))
        val aura = if (battle.status == "ended") battle.auraAwarded else BattleRules.auraFor(battle.score)
        Text(
            text = when {
                battle.isCancelled -> "Annulée par l'hôte : aucune aura gagnée"
                isHost && battle.status == "ended" -> "${aura.signed()} aura ajoutée à ton score"
                isHost -> "${aura.signed()} aura en cours de crédit…"
                else -> "${battle.hostName.ifBlank { "L'hôte" }} gagne ${aura.signed()} aura"
            },
            color = if (battle.isCancelled) AuraPalette.Negative else AuraPalette.Positive,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Taps", battle.taps.grouped(), Modifier.weight(1f))
            StatTile("Combo max", "x${maxOf(battle.maxCombo, myBestCombo)}", Modifier.weight(1f))
            StatTile("Supporters", supporters.size.toString(), Modifier.weight(1f))
        }
        if (mvp != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "👑 MVP : ${mvp.name.ifBlank { "Anonyme" }} (${mvp.points.signed()})",
                color = AuraPalette.TierSupreme,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
        if (mine != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ta contribution : ${mine.points.signed()} en ${mine.taps.grouped()} taps",
                color = AuraPalette.TextSecondary,
                fontSize = 14.sp,
            )
        }
        if (!battle.isCancelled) {
            Spacer(Modifier.height(14.dp))
            GradientButton(text = "✨ Partager les résultats", onClick = onShare)
        }
        Spacer(Modifier.height(18.dp))
        SupportersList(supporters, myUid, Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    AuraCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = AuraPalette.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label.uppercase(), color = AuraPalette.TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
        }
    }
}

private const val HYPE_MAX_TAPS_PER_SEC = 15f
