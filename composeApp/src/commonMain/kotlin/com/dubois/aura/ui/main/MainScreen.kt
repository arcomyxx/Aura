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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.time.Clock
import com.dubois.aura.model.AuraEvent
import com.dubois.aura.model.DailyLimits
import com.dubois.aura.model.RankedUser
import com.dubois.aura.ui.components.AuraHeader
import com.dubois.aura.ui.components.AuraTabBar
import com.dubois.aura.ui.theme.AuraPalette
import com.dubois.aura.ui.theme.colorFromHex
import com.dubois.aura.ui.theme.toHex
import com.dubois.aura.util.LocalShareBattle
import com.dubois.aura.util.LocalInviteToBattle
import com.dubois.aura.util.signed
import com.dubois.aura.viewmodel.BattleViewModel
import com.dubois.aura.viewmodel.MainViewModel

private val TABS = listOf("Profil", "Amis", "Battle", "Classement", "Groupes")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    battleViewModel: BattleViewModel,
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
    val dailyLimits by viewModel.dailyLimits.collectAsState()
    val selectedFriend by viewModel.selectedFriend.collectAsState()
    val friendEvents by viewModel.friendEvents.collectAsState()

    val groups by viewModel.groups.collectAsState()
    val groupInvites by viewModel.groupInvites.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val groupLeaderboard by viewModel.groupLeaderboard.collectAsState()
    val groupLoading by viewModel.groupLoading.collectAsState()

    val liveBattles by battleViewModel.liveBattles.collectAsState()
    val battleHistory by battleViewModel.history.collectAsState()
    val currentBattle by battleViewModel.current.collectAsState()
    val battleSupporters by battleViewModel.supporters.collectAsState()
    val battleNow by battleViewModel.now.collectAsState()
    val pendingPoints by battleViewModel.pendingPoints.collectAsState()
    val combo by battleViewModel.combo.collectAsState()
    val myBestCombo by battleViewModel.myBestCombo.collectAsState()
    val readyAt by battleViewModel.readyAt.collectAsState()
    val hype by battleViewModel.hype.collectAsState()
    val launching by battleViewModel.launching.collectAsState()
    val battleMessage by battleViewModel.message.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var reportTarget by remember { mutableStateOf<RankedUser?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<AuraEvent?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editReportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        viewModel.load()
        battleViewModel.load()
    }

    // Aura créditée à la fin d'une de mes battles -> message + rechargement du score.
    LaunchedEffect(Unit) {
        battleViewModel.auraEarned.collect { aura ->
            viewModel.showMessage("Battle terminée : ${aura.signed()} aura !")
            viewModel.load()
        }
    }
    LaunchedEffect(battleMessage) {
        battleMessage?.let {
            viewModel.showMessage(it)
            battleViewModel.clearMessage()
        }
    }

    // Horloge de l'onglet Battle (expiration des battles live, chronos).
    var tabNow by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(selectedTab) {
        while (selectedTab == 2) {
            tabNow = Clock.System.now().toEpochMilliseconds()
            delay(1_000)
        }
    }

    // Geste / bouton « retour » du système : ferme la battle ou le profil
    // d'ami affiché par-dessus les onglets au lieu de quitter l'application.
    BackHandler(enabled = currentBattle != null || selectedFriend != null || selectedGroup != null) {
        when {
            currentBattle != null -> battleViewModel.close()
            selectedFriend != null -> viewModel.closeFriend()
            else -> viewModel.closeGroup()
        }
    }

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
                auraScore = me?.profile?.auraScore,
                onEditProfile = { showEditProfile = true },
                onLogout = onLogout,
            )
            Spacer(Modifier.height(18.dp))

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
                2 -> {
                    val usersByUid = (friends + listOfNotNull(me)).associateBy { it.profile.uid }
                    val myUid = battleViewModel.myUid
                    BattleTab(
                        myLiveBattle = liveBattles.firstOrNull { it.hostUid == myUid && it.isLive(tabNow) },
                        friendLiveBattles = liveBattles.filter {
                            it.hostUid != myUid && it.hostUid in usersByUid && it.isLive(tabNow)
                        },
                        history = battleHistory,
                        usersByUid = usersByUid,
                        myUid = myUid,
                        now = tabNow,
                        launching = launching,
                        onLaunch = { battleViewModel.launch(me?.profile?.displayName ?: "") },
                        onOpen = { battleViewModel.open(it, me?.profile?.displayName ?: "") },
                        modifier = Modifier.weight(1f),
                    )
                }
                3 -> LeaderboardTab(
                    leaderboard = leaderboard,
                    modifier = Modifier.weight(1f),
                )
                else -> GroupsTab(
                    groups = groups,
                    groupInvites = groupInvites,
                    myUid = viewModel.myUid,
                    onCreateGroup = viewModel::createGroup,
                    onAcceptInvite = viewModel::acceptGroupInvite,
                    onDeclineInvite = viewModel::declineGroupInvite,
                    onOpenGroup = viewModel::openGroup,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))
            AuraTabBar(tabs = TABS, selected = selectedTab, onSelect = { selectedTab = it })
            Spacer(Modifier.height(8.dp))
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
                    dailyLimits = dailyLimits,
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

        // Groupe sélectionné (superposé par-dessus les onglets)
        selectedGroup?.let { group ->
            GroupDetailScreen(
                group = group,
                leaderboard = groupLeaderboard,
                loading = groupLoading,
                friends = friends,
                myUid = viewModel.myUid,
                onBack = { viewModel.closeGroup() },
                onInvite = { target -> viewModel.inviteToGroup(group, target) },
                onReport = { member, type, description ->
                    viewModel.reportGroupEvent(group.id, member.uid, type, description)
                },
                onLeave = { viewModel.leaveGroup(group) },
                onDelete = { viewModel.deleteGroup(group) },
            )
        }

        // Battle ouverte (superposée par-dessus les onglets)
        currentBattle?.let { battle ->
            val shareBattle = LocalShareBattle.current
            val inviteToBattle = LocalInviteToBattle.current
            BattleScreen(
                battle = battle,
                host = (friends + listOfNotNull(me)).firstOrNull { it.profile.uid == battle.hostUid },
                supporters = battleSupporters,
                myUid = battleViewModel.myUid,
                now = battleNow,
                pendingPoints = pendingPoints,
                combo = combo,
                myBestCombo = myBestCombo,
                readyAt = readyAt,
                hype = hype,
                onTap = battleViewModel::tap,
                onBack = battleViewModel::close,
                onCancel = battleViewModel::cancel,
                onShareBattle = {
                    shareBattle(
                        battle.hostName,
                        battle.score,
                        battleSupporters.size,
                        maxOf(battle.maxCombo, myBestCombo),
                        battle.auraAwarded,
                        battle.hostThemeColor,
                    )
                },
                onInvite = { inviteToBattle(battle.hostName) },
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
