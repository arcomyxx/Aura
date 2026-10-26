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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.ui.main.GradientButton
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.viewmodel.UserViewModel

@Composable
fun RegisterScreen(
    viewModel: UserViewModel,
    onRegisterSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val profile  by viewModel.profile.collectAsState()
    val error    by viewModel.error.collectAsState()

    LaunchedEffect(profile) {
        if (profile != null) onRegisterSuccess()
    }

    AuthScaffold(
        title = "Créer un compte",
        subtitle = "Ton aura commence ici.",
        onBack = onBackClick,
    ) {
        AuraTextField(value = name, onValueChange = { name = it }, placeholder = "Nom complet")
        Spacer(Modifier.height(12.dp))
        AuraTextField(value = email, onValueChange = { email = it }, placeholder = "Email")
        Spacer(Modifier.height(12.dp))
        AuraTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Mot de passe",
            visualTransformation = PasswordVisualTransformation(),
        )
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = AuraPalette.Negative,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(24.dp))
        GradientButton(text = "S'inscrire", onClick = { viewModel.register(email, password, name) })
    }
}
