package com.dubois.aura.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.dubois.aura.model.AuraEvent
import com.dubois.aura.model.RankedUser
import com.dubois.aura.ui.components.AuraHeader
import com.dubois.aura.ui.components.AuraTabBar
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.colorFromHex
import com.dubois.aura.viewmodel.MainViewModel

private val TABS = listOf("Profil", "Amis", "Classement")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit,
) {
    val me by viewModel.me.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val events by viewModel.events.collectAsState()
    val weeklyDelta by viewModel.weeklyDelta.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val requests by viewModel.requests.collectAsState()
    val searching by viewModel.searching.collectAsState()
    val message by viewModel.message.collectAsState()
    val selectedFriend by viewModel.selectedFriend.collectAsState()
    val friendEvents by viewModel.friendEvents.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var reportTarget by remember { mutableStateOf<RankedUser?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<AuraEvent?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editReportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { viewModel.load() }

    // Applique la couleur d'accent choisie par l'utilisateur à tout le thème.
    LaunchedEffect(me?.profile?.themeColor) {
        AuraPalette.setAccent(colorFromHex(me?.profile?.themeColor ?: ""))
    }

    fun openSheet(target: RankedUser?) {
        reportTarget = target
        showSheet = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraPalette.Background)
            .background(
                Brush.radialGradient(
                    colors = listOf(AuraPalette.BackgroundGlow, AuraPalette.Background),
                    center = Offset(0.5f * 1000f, 0f),
                    radius = 900f,
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(horizontal = 20.dp),
        ) {
            AuraHeader(
                displayName = me?.profile?.displayName ?: "",
                photoBase64 = me?.profile?.photoUrl ?: "",
                onEditProfile = { showEditProfile = true },
                onLogout = onLogout,
            )
            Spacer(Modifier.height(18.dp))
            AuraTabBar(tabs = TABS, selected = selectedTab, onSelect = { selectedTab = it })
            Spacer(Modifier.height(20.dp))

            when (selectedTab) {
                0 -> ProfileTab(
                    me = me,
                    events = events,
                    weeklyDelta = weeklyDelta,
                    modifier = Modifier.weight(1f),
                )
                1 -> FriendsTab(
                    friends = friends,
                    searchResults = searchResults,
                    requests = requests,
                    searching = searching,
                    onSearch = viewModel::search,
                    onClearSearch = viewModel::clearSearch,
                    onSendRequest = viewModel::sendRequest,
                    onAccept = viewModel::accept,
                    onDecline = viewModel::decline,
                    onReport = { openSheet(it) },
                    onOpenProfile = { viewModel.openFriend(it) },
                    modifier = Modifier.weight(1f),
                )
                else -> LeaderboardTab(
                    leaderboard = leaderboard,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = AuraPalette.Surface,
            ) {
                ReportEventSheetContent(
                    initialTarget = reportTarget,
                    friends = friends,
                    onDismiss = { showSheet = false },
                    onSubmit = { target, type, description ->
                        viewModel.report(target, type, description)
                        showSheet = false
                    },
                )
            }
        }

        if (showEditProfile) {
            ModalBottomSheet(
                onDismissRequest = { showEditProfile = false },
                sheetState = editSheetState,
                containerColor = AuraPalette.Surface,
            ) {
                EditProfileSheetContent(
                    profile = me?.profile,
                    onDismiss = { showEditProfile = false },
                    onSubmit = { name, bio, photoUrl, themeColor ->
                        viewModel.updateProfile(name, bio, photoUrl, themeColor)
                        showEditProfile = false
                    },
                )
            }
        }

        // Profil d'un ami (superposé par-dessus les onglets)
        selectedFriend?.let { friend ->
            FriendProfileScreen(
                friend = friend,
                events = friendEvents,
                myUid = viewModel.myUid,
                onBack = { viewModel.closeFriend() },
                onEditEvent = { editingEvent = it },
                onDeleteEvent = { viewModel.deleteEvent(it) },
            )
        }

        if (editingEvent != null) {
            val event = editingEvent!!
            ModalBottomSheet(
                onDismissRequest = { editingEvent = null },
                sheetState = editReportSheetState,
                containerColor = AuraPalette.Surface,
            ) {
                EditReportSheetContent(
                    event = event,
                    onDismiss = { editingEvent = null },
                    onSubmit = { type, description ->
                        viewModel.updateEvent(event, type, description)
                        editingEvent = null
                    },
                )
            }
        }

        // Bandeau de message transitoire (demande envoyée, ami ajouté, erreur…)
        message?.let { text ->
            LaunchedEffect(text) {
                delay(2800)
                viewModel.clearMessage()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeContentPadding()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AuraPalette.SurfaceElevated)
                    .border(BorderStroke(1.dp, AuraPalette.CardBorder), RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Text(text, color = AuraPalette.TextPrimary, fontSize = 14.sp)
            }
        }
    }
}
