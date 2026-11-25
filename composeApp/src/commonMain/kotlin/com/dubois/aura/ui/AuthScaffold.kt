package com.dubois.aura.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.ui.components.AuraIcon
import com.dubois.aura.ui.components.IconCircleButton
import com.dubois.aura.ui.theme.AuraPalette

/** Fond commun aux écrans hors connexion : fond sombre + halo orangé en haut. */
fun Modifier.auraScreenBackground(): Modifier = this
    .background(AuraPalette.Background)
    .background(
        Brush.radialGradient(
            colors = listOf(AuraPalette.BackgroundGlow, AuraPalette.Background),
            center = Offset(0.5f * 1000f, 0f),
            radius = 900f,
        )
    )

/**
 * Gabarit des écrans de connexion / inscription : bouton retour, titre et
 * sous-titre centrés, puis le formulaire.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().auraScreenBackground()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            IconCircleButton(icon = AuraIcon.Back, onClick = onBack)
            Spacer(Modifier.height(40.dp))
            Text(
                text = title,
                color = AuraPalette.TextPrimary,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = AuraPalette.TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, content = content)
        }
    }
}

/** Champ de saisie thémé (fond surface, bordure accent au focus). */
@Composable
fun AuraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    error: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = AuraPalette.TextMuted) },
        singleLine = true,
        visualTransformation = visualTransformation,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = auraTextFieldColors(),
        isError = error != null,
        supportingText = error?.let { { Text(it, color = AuraPalette.Negative, fontSize = 12.sp) } },
    )
}

/** Traduit les messages d'erreur Firebase en français. */
fun localizeFirebaseError(raw: String): String = when {
    raw.contains("INVALID_EMAIL", ignoreCase = true)
        || raw.contains("invalid-email", ignoreCase = true)       -> "Adresse email invalide."
    raw.contains("EMAIL_EXISTS", ignoreCase = true)
        || raw.contains("email-already-in-use", ignoreCase = true) -> "Cette adresse est déjà utilisée."
    raw.contains("WRONG_PASSWORD", ignoreCase = true)
        || raw.contains("invalid-credential", ignoreCase = true)
        || raw.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) -> "Email ou mot de passe incorrect."
    raw.contains("USER_NOT_FOUND", ignoreCase = true)
        || raw.contains("user-not-found", ignoreCase = true)       -> "Aucun compte trouvé avec cet email."
    raw.contains("WEAK_PASSWORD", ignoreCase = true)
        || raw.contains("weak-password", ignoreCase = true)        -> "Mot de passe trop faible (8 caractères min.)."
    raw.contains("TOO_MANY_ATTEMPTS", ignoreCase = true)
        || raw.contains("too-many-requests", ignoreCase = true)    -> "Trop de tentatives. Réessaie dans quelques minutes."
    raw.contains("NETWORK_REQUEST_FAILED", ignoreCase = true)
        || raw.contains("network-request-failed", ignoreCase = true) -> "Problème réseau. Vérifie ta connexion."
    else -> "Une erreur est survenue. Réessaie."
}

@Composable
fun auraTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AuraPalette.TextPrimary,
    unfocusedTextColor = AuraPalette.TextPrimary,
    focusedContainerColor = AuraPalette.Surface,
    unfocusedContainerColor = AuraPalette.Surface,
    focusedBorderColor = AuraPalette.Primary,
    unfocusedBorderColor = AuraPalette.CardBorder,
    cursorColor = AuraPalette.Primary,
)
