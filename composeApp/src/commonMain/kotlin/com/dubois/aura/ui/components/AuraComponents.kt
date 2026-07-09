package com.dubois.aura.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.ui.theme.AuraPalette

/** Retourne les initiales (1 à 2 lettres) d'un nom d'affichage. */
fun initialsOf(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

/** Avatar circulaire à initiales, avec anneau coloré (couleur de tier). */
@Composable
fun InitialsAvatar(
    name: String,
    ringColor: Color,
    modifier: Modifier = Modifier,
    size: Int = 44,
    fontSize: Int = 15,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(ringColor.copy(alpha = 0.14f))
            .border(BorderStroke(1.5.dp, ringColor.copy(alpha = 0.7f)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialsOf(name),
            color = ringColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Petit titre de section en capitales espacées (ex. "ÉVÉNEMENTS RÉCENTS"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = AuraPalette.TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        modifier = modifier,
    )
}

/** Carte sombre standard (fond surface + bordure discrète). */
@Composable
fun AuraCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlighted) AuraPalette.SurfaceElevated else AuraPalette.Surface)
            .border(
                BorderStroke(
                    1.dp,
                    if (highlighted) AuraPalette.Primary.copy(alpha = 0.5f) else AuraPalette.CardBorder
                ),
                RoundedCornerShape(18.dp)
            )
    ) { content() }
}

/**
 * Barre d'onglets segmentée (Profil / Amis / Classement). Le segment actif
 * porte un dégradé violet.
 */
@Composable
fun AuraTabBar(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(AuraPalette.TabTrack)
            .border(BorderStroke(1.dp, AuraPalette.CardBorder), RoundedCornerShape(28.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, label ->
            TabSegment(
                label = label,
                active = index == selected,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TabSegment(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val base = modifier
        .clip(shape)
        .height(44.dp)
        .clickable(onClick = onClick)
    val filled = if (active) {
        base.background(
            Brush.horizontalGradient(
                listOf(AuraPalette.PrimaryGradientStart, AuraPalette.PrimaryGradientEnd)
            )
        )
    } else base
    Box(filled, contentAlignment = Alignment.Center) {
        Text(
            text = label,
            color = if (active) AuraPalette.OnPrimary else AuraPalette.TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * En-tête commun aux trois onglets : label "AURA SCORE", nom en serif, avatar.
 */
@Composable
fun AuraHeader(
    displayName: String,
    modifier: Modifier = Modifier,
    onAvatarClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            SectionLabel("Aura Score")
            Text(
                text = displayName.ifBlank { "—" },
                color = AuraPalette.TextPrimary,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
            )
        }
        InitialsAvatar(
            name = displayName,
            ringColor = AuraPalette.Primary,
            size = 48,
            fontSize = 16,
            modifier = Modifier.clickableNoRipple(onAvatarClick),
        )
    }
}

/** Clic sans ondulation (pour les libellés-actions type "+ Signaler"). */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.composed {
        val interaction = remember { MutableInteractionSource() }
        clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
    }
