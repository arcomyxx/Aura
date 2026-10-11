package com.dubois.aura.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubois.aura.model.AuraEvent
import com.dubois.aura.model.EventCatalog
import com.dubois.aura.model.EventType
import com.dubois.aura.ui.theme.AuraPalette

/**
 * Contenu de la feuille « Modifier le signalement » : change le type, le
 * montant et la description d'un événement dont l'utilisateur courant est l'auteur.
 */
@Composable
fun EditReportSheetContent(
    event: AuraEvent,
    onDismiss: () -> Unit,
    onSubmit: (type: EventType, description: String) -> Unit,
) {
    var selectedType by remember {
        mutableStateOf(EventCatalog.all.firstOrNull { it.id == event.typeId })
    }
    var description by remember { mutableStateOf(event.description) }
    var amount by remember { mutableStateOf(kotlin.math.abs(event.delta).toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Modifier le signalement",
                color = AuraPalette.TextPrimary,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.weight(1f),
            )
            CloseButtonEditReport(onDismiss)
        }
        Spacer(Modifier.height(20.dp))

        EventCatalog.all.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { type ->
                    EventTypeCard(
                        type = type,
                        selected = selectedType?.id == type.id,
                        onClick = {
                            selectedType = type
                            amount = kotlin.math.abs(type.delta).toString()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        selectedType?.let { type ->
            AuraAmountField(
                amount = amount,
                onAmountChange = { amount = it },
                positive = type.isPositive,
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Décris l'événement (optionnel)…", color = AuraPalette.TextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AuraPalette.TextPrimary,
                unfocusedTextColor = AuraPalette.TextPrimary,
                focusedContainerColor = AuraPalette.Surface,
                unfocusedContainerColor = AuraPalette.Surface,
                focusedBorderColor = AuraPalette.Primary,
                unfocusedBorderColor = AuraPalette.CardBorder,
                cursorColor = AuraPalette.Primary,
            ),
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val type = selectedType ?: return@Button
                val value = parseAuraAmount(amount) ?: return@Button
                onSubmit(type.withAmount(value), description)
            },
            enabled = selectedType != null && parseAuraAmount(amount) != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AuraPalette.Primary,
                contentColor = AuraPalette.OnPrimary,
                disabledContainerColor = AuraPalette.SurfaceElevated,
                disabledContentColor = AuraPalette.TextMuted,
            ),
        ) {
            Text("Enregistrer", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun CloseButtonEditReport(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(AuraPalette.SurfaceElevated)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", color = AuraPalette.TextSecondary, fontSize = 15.sp)
    }
}
