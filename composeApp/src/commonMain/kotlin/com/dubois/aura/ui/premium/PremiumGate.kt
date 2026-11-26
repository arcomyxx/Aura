package com.dubois.aura.ui.premium

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dubois.aura.repository.PremiumStatus
import com.dubois.aura.viewmodel.PremiumViewModel

/**
 * Affiche [content] si l'utilisateur est Premium, sinon affiche [locked].
 * [locked] reçoit une lambda pour ouvrir le paywall.
 */
@Composable
fun PremiumGate(
    premiumViewModel: PremiumViewModel = viewModel(),
    locked: @Composable (openPaywall: () -> Unit) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val status by premiumViewModel.premiumStatus.collectAsState()

    when (status) {
        is PremiumStatus.Active -> content()
        else -> locked { /* le parent gère la navigation vers le paywall */ }
    }
}
