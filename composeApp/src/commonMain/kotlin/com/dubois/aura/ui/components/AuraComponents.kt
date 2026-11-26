package com.dubois.aura.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.util.decodeImageBitmap

/** Retourne les initiales (1 à 2 lettres) d'un nom d'affichage. */
fun initialsOf(name: String): String {
    // Première lettre de chaque mot, en ignorant les mots sans lettre (emojis…)
    // pour ne jamais couper un emoji en deux (« M� »).
    val letters = name.trim().split(" ").mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
    return when {
        letters.isEmpty() -> "?"
        letters.size == 1 -> letters[0].uppercase()
        else -> (letters[0].uppercase() + letters[1].uppercase())
    }
}

/**
 * Avatar circulaire, avec anneau coloré (couleur de tier). Affiche la photo de
 * profil ([photoBase64], stockée en base64) si elle est présente et décodable,
 * sinon les initiales du nom. Si [auraScore] est fourni, l'avatar est entouré
 * de l'aura animée correspondant à ce score.
 */
@Composable
fun InitialsAvatar(
    name: String,
    ringColor: Color,
    modifier: Modifier = Modifier,
    size: Int = 44,
    fontSize: Int = 15,
    photoBase64: String = "",
    auraScore: Int? = null,
) {
    val bitmap = remember(photoBase64) { decodeImageBitmap(photoBase64) }
    Box(
        modifier = modifier
            .then(if (auraScore != null) Modifier.auraFlame(auraScore, compact = size < 80) else Modifier)
            .size(size.dp)
            .clip(CircleShape)
            // fond opaque : l'aura dessinée derrière ne doit pas transparaître
            .background(AuraPalette.Surface)
            .background(ringColor.copy(alpha = 0.14f))
            .border(BorderStroke(1.5.dp, ringColor.copy(alpha = 0.7f)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Text(
                text = initialsOf(name),
                color = ringColor,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
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

/**
 * Carte sombre standard (fond surface + bordure discrète). [accent] tinte la
 * bordure (ex. couleur de thème d'un ami) ; [highlighted] met en avant la carte.
 */
@Composable
fun AuraCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    val borderColor = when {
        accent != null -> accent.copy(alpha = 0.55f)
        highlighted -> AuraPalette.Primary.copy(alpha = 0.5f)
        else -> AuraPalette.CardBorder
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlighted) AuraPalette.SurfaceElevated else AuraPalette.Surface)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
    ) { content() }
}

/**
 * Barre de navigation du bas style "modern mobile" : icône + label pour chaque
 * onglet, FAB gradient centré qui remonte au-dessus de la barre. Les 4 onglets
 * sont répartis 2 à gauche / 2 à droite du FAB.
 *
 * [tabs] : liste de (label, icône Unicode), 4 éléments attendus.
 * [onActionClick] : callback du bouton central (Signaler).
 */
@Composable
fun AuraBottomNav(
    tabs: List<Pair<String, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onActionClick: () -> Unit,
    actionEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    // Hauteur visible de la barre + espace au-dessus pour le FAB.
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // ─── Barre de fond ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(AuraPalette.Surface)
        ) {
            // Trait supérieur
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AuraPalette.CardBorder)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 2 onglets de gauche
                tabs.take(2).forEachIndexed { idx, (label, icon) ->
                    BottomNavItem(
                        icon = icon,
                        label = label,
                        active = idx == selected,
                        onClick = { onSelect(idx) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Espace réservé pour le FAB
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1.1f))
                // 2 onglets de droite
                tabs.drop(2).forEachIndexed { i, (label, icon) ->
                    BottomNavItem(
                        icon = icon,
                        label = label,
                        active = (i + 2) == selected,
                        onClick = { onSelect(i + 2) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ─── FAB central ────────────────────────────────────────────────
        if (actionEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(bottom = 32.dp)   // remonte au-dessus de la barre
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                AuraPalette.PrimaryGradientStart,
                                AuraPalette.PrimaryGradientEnd,
                            )
                        )
                    )
                    .border(
                        BorderStroke(2.dp, AuraPalette.Surface),
                        CircleShape,
                    )
                    .clickable(onClick = onActionClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "⚡",
                    fontSize = 22.sp,
                    color = AuraPalette.OnPrimary,
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: String,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (active) AuraPalette.Primary else AuraPalette.TextMuted
    val weight = if (active) FontWeight.SemiBold else FontWeight.Normal
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .height(68.dp)
            .clickableNoRipple(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Pastille indicateur actif
        Box(
            Modifier
                .size(width = 24.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (active) AuraPalette.Primary else Color.Transparent)
        )
        androidx.compose.foundation.layout.Spacer(
            Modifier.height(6.dp)
        )
        Text(icon, fontSize = 20.sp, color = color)
        androidx.compose.foundation.layout.Spacer(Modifier.height(3.dp))
        Text(
            label,
            fontSize = 10.sp,
            color = color,
            fontWeight = weight,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * En-tête commun aux trois onglets : label "AURA SCORE", nom en serif, avatar.
 * Le clic sur l'avatar ouvre un menu (modifier le profil / compte).
 */
@Composable
fun AuraHeader(
    displayName: String,
    modifier: Modifier = Modifier,
    photoBase64: String = "",
    auraScore: Int? = null,
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }

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
        Box {
            InitialsAvatar(
                name = displayName,
                ringColor = AuraPalette.Primary,
                size = 48,
                fontSize = 16,
                photoBase64 = photoBase64,
                auraScore = auraScore,
                modifier = Modifier.clickableNoRipple { menuOpen = true },
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = AuraPalette.SurfaceElevated,
            ) {
                DropdownMenuItem(
                    text = { Text("Mon compte", color = AuraPalette.TextPrimary, fontSize = 14.sp) },
                    onClick = {
                        menuOpen = false
                        onOpenAccount()
                    },
                    colors = MenuDefaults.itemColors(textColor = AuraPalette.TextPrimary),
                )
            }
        }
    }
}

/**
 * Pour un écran superposé aux onglets : intercepte les touches pour qu'elles
 * n'atteignent pas l'écran situé dessous (un fond seul ne les bloque pas).
 */
fun Modifier.blockTouchesBehind(): Modifier = this.pointerInput(Unit) {}

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
