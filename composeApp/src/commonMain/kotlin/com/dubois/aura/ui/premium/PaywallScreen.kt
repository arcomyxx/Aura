package com.dubois.aura.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dubois.aura.repository.PremiumStatus
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.PremiumColors
import com.dubois.aura.viewmodel.PremiumViewModel
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions

@Composable
fun PaywallScreen(
    onDismiss: () -> Unit,
    premiumViewModel: PremiumViewModel = viewModel(),
) {
    val premiumStatus by premiumViewModel.premiumStatus.collectAsState()

    // Fermer automatiquement quand l'achat réussit
    LaunchedEffect(premiumStatus) {
        if (premiumStatus is PremiumStatus.Active) onDismiss()
    }

    Paywall(PaywallOptions(dismissRequest = onDismiss))
}

/** Bannière "Passe à Premium" compacte pour insérer inline (ex: dans GroupsTab). */
@Composable
fun PremiumUpsellBanner(
    message: String,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF2A1A00), Color(0xFF3D2800), Color(0xFF2A1A00))
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "✦ Premium",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PremiumColors.BadgeGold,
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = AuraPalette.TextSecondary,
            )
        }
        Button(
            onClick = onUpgrade,
            colors = ButtonDefaults.buttonColors(containerColor = PremiumColors.BadgeGold),
        ) {
            Text("Découvrir", color = Color(0xFF1A0F02), fontWeight = FontWeight.Bold)
        }
    }
}
