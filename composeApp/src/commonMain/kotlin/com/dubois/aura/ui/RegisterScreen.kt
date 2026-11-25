package com.dubois.aura.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.ui.main.GradientButton
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.viewmodel.UserViewModel

private const val PRIVACY_URL = "https://aura-7d0fb.web.app/privacy"

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

@Composable
fun RegisterScreen(
    viewModel: UserViewModel,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var touched  by remember { mutableStateOf(false) }
    val profile  by viewModel.profile.collectAsState()
    val error    by viewModel.error.collectAsState()

    LaunchedEffect(profile) {
        if (profile != null) onRegisterSuccess()
    }

    val uriHandler = LocalUriHandler.current
    val nameError     = if (touched && name.trim().isEmpty()) "Le nom est requis" else null
    val emailError    = if (touched && !EMAIL_REGEX.matches(email.trim())) "Email invalide" else null
    val passwordError = if (touched && password.length < 8) "Au moins 8 caractères" else null

    AuthScaffold(
        title = "Créer un compte",
        subtitle = "Ton aura commence ici.",
        onBack = onBackClick,
    ) {
        AuraTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Nom complet",
            error = nameError,
        )
        Spacer(Modifier.height(12.dp))
        AuraTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Email",
            error = emailError,
        )
        Spacer(Modifier.height(12.dp))
        AuraTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Mot de passe (8 caractères min.)",
            visualTransformation = PasswordVisualTransformation(),
            error = passwordError,
        )
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                localizeFirebaseError(it),
                color = AuraPalette.Negative,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))
        val privacyText = buildAnnotatedString {
            append("En créant un compte, tu acceptes notre ")
            withStyle(SpanStyle(color = AuraPalette.Primary)) {
                append("politique de confidentialité")
            }
            append(".")
        }
        Text(
            text = privacyText,
            color = AuraPalette.TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { uriHandler.openUri(PRIVACY_URL) },
        )
        Spacer(Modifier.height(16.dp))
        GradientButton(
            text = "S'inscrire",
            onClick = {
                touched = true
                val valid = name.trim().isNotEmpty()
                    && EMAIL_REGEX.matches(email.trim())
                    && password.length >= 8
                if (valid) viewModel.register(email.trim(), password, name.trim())
            },
        )
        Spacer(Modifier.height(12.dp))
        GoogleSignInButton(onIdToken = { viewModel.loginWithGoogle(it) })
    }
}
