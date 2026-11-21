package com.dubois.aura.ui

import androidx.compose.runtime.Composable

/** Bouton « Continuer avec Google » ; implémenté uniquement sur Android. */
@Composable
expect fun GoogleSignInButton(onIdToken: (String) -> Unit)
