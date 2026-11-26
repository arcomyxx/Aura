package com.dubois.aura.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.ui.theme.PremiumColors

/**
 * Badge ✦ affiché à côté du nom des utilisateurs Premium.
 * Taille configurable selon le contexte (profil vs classement).
 */
@Composable
fun PremiumBadge(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 10.sp,
    horizontalPadding: Dp = 5.dp,
    verticalPadding: Dp = 2.dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(PremiumColors.BadgeGold, PremiumColors.BadgeGoldLight, PremiumColors.BadgeGold)
                )
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "✦",
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color(0xFF1A0F02),
        )
    }
}
