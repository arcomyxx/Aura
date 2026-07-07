package com.dubois.aura.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dubois.aura.model.Friend
import com.dubois.aura.model.FriendRequest
import com.dubois.aura.model.UserProfile
import com.dubois.aura.viewmodel.FriendsViewModel

@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onBackClick: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val friends by viewModel.friends.collectAsState()
    val requests by viewModel.requests.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val message by viewModel.message.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mes amis", style = MaterialTheme.typography.headlineMedium)

        // ── Recherche pour envoyer une demande ────────────────────
        Text("Ajouter un ami", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Nom ou email") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { viewModel.search(query) },
                enabled = query.isNotBlank() && !loading
            ) {
                Text("Chercher")
            }
        }

        message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Résultats de recherche ────────────────────────────
            if (loading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                }
            }
            if (results.isNotEmpty()) {
                item { SectionTitle("Résultats") }
                items(results, key = { it.uid }) { user ->
                    SearchResultRow(user) { viewModel.sendRequest(user) }
                }
                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            }

            // ── Demandes reçues ───────────────────────────────────
            item { SectionTitle("Demandes reçues (${requests.size})") }
            if (requests.isEmpty()) {
                item { EmptyHint("Aucune demande en attente") }
            } else {
                items(requests, key = { it.id }) { request ->
                    RequestRow(
                        request = request,
                        onAccept = { viewModel.accept(request) },
                        onDecline = { viewModel.decline(request) }
                    )
                }
            }

            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

            // ── Liste d'amis ──────────────────────────────────────
            item { SectionTitle("Amis (${friends.size})") }
            if (friends.isEmpty()) {
                item { EmptyHint("Vous n'avez pas encore d'amis") }
            } else {
                items(friends, key = { it.uid }) { friend ->
                    FriendRow(friend)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        TextButton(onClick = onBackClick) { Text("Retour") }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
private fun SearchResultRow(user: UserProfile, onAdd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    user.displayName.ifEmpty { "(sans nom)" },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Button(onClick = onAdd) { Text("Ajouter") }
        }
    }
}

@Composable
private fun RequestRow(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    request.fromName.ifEmpty { request.fromEmail },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    request.fromEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            OutlinedButton(onClick = onDecline) { Text("Refuser") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onAccept) { Text("Accepter") }
        }
    }
}

@Composable
private fun FriendRow(friend: Friend) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                friend.displayName.ifEmpty { "(sans nom)" },
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                friend.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
