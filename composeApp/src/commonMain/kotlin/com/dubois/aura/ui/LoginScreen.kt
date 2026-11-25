package com.dubois.aura.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.ui.main.GradientButton
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.viewmodel.UserViewModel

@Composable
fun LoginScreen(
    viewModel: UserViewModel,
    onLoginSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val profile  by viewModel.profile.collectAsState()
    val error    by viewModel.error.collectAsState()
    val resetSent by viewModel.resetEmailSent.collectAsState()

    LaunchedEffect(profile) {
        if (profile != null) onLoginSuccess()
    }

    AuthScaffold(
        title = "Connexion",
        subtitle = "Content de revoir ton aura.",
        onBack = onBackClick,
    ) {
        AuraTextField(value = email, onValueChange = { email = it }, placeholder = "Email")
        Spacer(Modifier.height(12.dp))
        AuraTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Mot de passe",
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = { viewModel.sendPasswordReset(email) },
            enabled = email.isNotBlank(),
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(
                "Mot de passe oublié ?",
                color = if (email.isNotBlank()) AuraPalette.Primary else AuraPalette.TextMuted,
                fontSize = 13.sp,
            )
        }
        if (resetSent) {
            Text(
                "Un email de réinitialisation a été envoyé.",
                color = AuraPalette.Positive,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        error?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                localizeFirebaseError(it),
                color = AuraPalette.Negative,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))
        GradientButton(
            text = "Se connecter",
            onClick = { viewModel.login(email, password) },
        )
        Spacer(Modifier.height(12.dp))
        GoogleSignInButton(onIdToken = { viewModel.loginWithGoogle(it) })
    }
}
