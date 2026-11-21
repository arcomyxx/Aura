package com.dubois.aura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dubois.aura.model.AuraEvent
import com.dubois.aura.model.DailyLimits
import com.dubois.aura.model.EventType
import com.dubois.aura.model.FriendRequest
import com.dubois.aura.model.Group
import com.dubois.aura.model.GroupInvite
import com.dubois.aura.model.GroupMember
import com.dubois.aura.model.RankedUser
import com.dubois.aura.model.UserProfile
import com.dubois.aura.repository.EventRepository
import com.dubois.aura.repository.FriendRepository
import com.dubois.aura.repository.GroupRepository
import com.dubois.aura.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * ViewModel de l'expérience principale à onglets (Profil / Amis / Classement).
 * Charge le profil courant, le classement global, la liste d'amis enrichie de
 * leur rang, ainsi que les événements reçus ; gère aussi le signalement.
 */
class MainViewModel : ViewModel() {

    private val userRepo = UserRepository()
    private val friendRepo = FriendRepository()
    private val eventRepo = EventRepository()
    private val groupRepo = GroupRepository()

    private val _me = MutableStateFlow<RankedUser?>(null)
    val me: StateFlow<RankedUser?> = _me

    private val _leaderboard = MutableStateFlow<List<RankedUser>>(emptyList())
    val leaderboard: StateFlow<List<RankedUser>> = _leaderboard

    private val _friends = MutableStateFlow<List<RankedUser>>(emptyList())
    val friends: StateFlow<List<RankedUser>> = _friends

    /** Demandes d'ami reçues en attente. */
    private val _requests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val requests: StateFlow<List<FriendRequest>> = _requests

    /** Résultats de la recherche d'utilisateurs. */
    private val _searchResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val searchResults: StateFlow<List<UserProfile>> = _searchResults

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching

    private val _events = MutableStateFlow<List<AuraEvent>>(emptyList())
    val events: StateFlow<List<AuraEvent>> = _events

    /** Somme des deltas reçus sur les 7 derniers jours. */
    private val _weeklyDelta = MutableStateFlow(0)
    val weeklyDelta: StateFlow<Int> = _weeklyDelta

    /** Ami dont on consulte le profil (null = aucun profil ouvert). */
    private val _selectedFriend = MutableStateFlow<RankedUser?>(null)
    val selectedFriend: StateFlow<RankedUser?> = _selectedFriend

    /** Historique d'événements de l'ami consulté. */
    private val _friendEvents = MutableStateFlow<List<AuraEvent>>(emptyList())
    val friendEvents: StateFlow<List<AuraEvent>> = _friendEvents

    /** UID de l'utilisateur courant, pour distinguer mes signalements des autres. */
    val myUid: String? get() = userRepo.currentUser()?.uid

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // ── Groupes ───────────────────────────────────────────────────────
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    private val _groupInvites = MutableStateFlow<List<GroupInvite>>(emptyList())
    val groupInvites: StateFlow<List<GroupInvite>> = _groupInvites

    /** Groupe dont on consulte le classement (null = liste des groupes). */
    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup

    /** Classement des membres du groupe sélectionné (scores propres au groupe). */
    private val _groupLeaderboard = MutableStateFlow<List<GroupMember>>(emptyList())
    val groupLeaderboard: StateFlow<List<GroupMember>> = _groupLeaderboard

    private val _groupLoading = MutableStateFlow(false)
    val groupLoading: StateFlow<Boolean> = _groupLoading

    private val _dailyLimits = MutableStateFlow(DailyLimits())
    val dailyLimits: StateFlow<DailyLimits> = _dailyLimits

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            val myUid = userRepo.currentUser()?.uid

            // Rang global (parmi tous les utilisateurs) -> profil & onglet Amis.
            val all = userRepo.allUsersRanked()
            val ranked = all.mapIndexed { index, profile ->
                RankedUser(profile, rank = index + 1, isMe = profile.uid == myUid)
            }
            _me.value = ranked.firstOrNull { it.isMe }

            // Amis = intersection classement global / liste d'amis, dans l'ordre du classement.
            val friendUids = friendRepo.friends().map { it.uid }.toSet()
            _friends.value = ranked.filter { it.profile.uid in friendUids }

            // Classement affiché = moi + mes amis uniquement, re-classés 1..N
            // (exclut les autres utilisateurs, dont les comptes de test).
            _leaderboard.value = all
                .filter { it.uid == myUid || it.uid in friendUids }
                .mapIndexed { index, profile ->
                    RankedUser(profile, rank = index + 1, isMe = profile.uid == myUid)
                }

            // Demandes d'ami reçues.
            _requests.value = friendRepo.incomingRequests()

            // Groupes & invitations.
            groupRepo.myGroups().onSuccess { _groups.value = it }
            groupRepo.incomingInvites().onSuccess { _groupInvites.value = it }

            // Événements reçus + delta hebdo.
            if (myUid != null) {
                val recent = eventRepo.recentEvents(myUid)
                _events.value = recent
                val weekAgo = Clock.System.now().toEpochMilliseconds() - 7L * 24 * 60 * 60 * 1000
                _weeklyDelta.value = recent.filter { it.createdAt >= weekAgo }.sumOf { it.delta }
            }

            _dailyLimits.value = eventRepo.myDailyLimits()
            _loading.value = false
        }
    }

    // ── Recherche & demandes d'ami ─────────────────────────────────

    /** Recherche des utilisateurs par nom ou email (correspondance exacte). */
    fun search(query: String) {
        viewModelScope.launch {
            _searching.value = true
            friendRepo.searchUsers(query)
                .onSuccess { _searchResults.value = it }
                .onFailure { _message.value = it.message }
            _searching.value = false
        }
    }

    fun clearSearch() { _searchResults.value = emptyList() }

    /** Envoie une demande d'ami vers [target]. */
    fun sendRequest(target: UserProfile) {
        viewModelScope.launch {
            friendRepo.sendFriendRequest(target)
                .onSuccess {
                    _message.value = "Demande envoyée à ${target.displayName.ifBlank { target.email }}"
                    _searchResults.value = _searchResults.value.filterNot { it.uid == target.uid }
                }
                .onFailure { _message.value = it.message }
        }
    }

    /** Accepte une demande d'ami puis recharge tout. */
    fun accept(request: FriendRequest) {
        viewModelScope.launch {
            friendRepo.acceptRequest(request)
                .onSuccess {
                    _message.value = "${request.fromName.ifBlank { request.fromEmail }} est maintenant votre ami"
                    load()
                }
                .onFailure { _message.value = it.message }
        }
    }

    /** Refuse une demande d'ami. */
    fun decline(request: FriendRequest) {
        viewModelScope.launch {
            friendRepo.declineRequest(request)
                .onSuccess { _requests.value = _requests.value.filterNot { it.id == request.id } }
                .onFailure { _message.value = it.message }
        }
    }

    // ── Profil d'un ami & gestion de mes signalements ──────────────

    /** Ouvre le profil d'un ami et charge son historique d'événements. */
    fun openFriend(friend: RankedUser) {
        _selectedFriend.value = friend
        _friendEvents.value = emptyList()
        viewModelScope.launch {
            _friendEvents.value = eventRepo.recentEvents(friend.profile.uid)
        }
    }

    /** Ferme le profil d'ami consulté. */
    fun closeFriend() {
        _selectedFriend.value = null
        _friendEvents.value = emptyList()
    }

    /** Recharge l'historique et le score de l'ami consulté après une modif. */
    private fun refreshFriendProfile() {
        val current = _selectedFriend.value ?: return
        viewModelScope.launch {
            _friendEvents.value = eventRepo.recentEvents(current.profile.uid)
            val fresh = userRepo.getProfile(current.profile.uid)
            _selectedFriend.value = current.copy(profile = fresh)
        }
    }

    /** Modifie un de mes signalements (type + description) sur l'ami consulté. */
    fun updateEvent(event: AuraEvent, type: EventType, description: String) {
        viewModelScope.launch {
            eventRepo.updateEvent(event, type, description)
                .onSuccess {
                    _message.value = "Signalement modifié"
                    refreshFriendProfile()
                    load()
                }
                .onFailure { _message.value = it.message }
        }
    }

    /** Supprime un de mes signalements sur l'ami consulté. */
    fun deleteEvent(event: AuraEvent) {
        viewModelScope.launch {
            eventRepo.deleteEvent(event)
                .onSuccess {
                    _message.value = "Signalement supprimé"
                    refreshFriendProfile()
                    load()
                }
                .onFailure { _message.value = it.message }
        }
    }

    /** Met à jour le profil courant (nom + bio + photo + couleur), puis recharge. */
    fun updateProfile(name: String, bio: String, photoUrl: String, themeColor: String) {
        viewModelScope.launch {
            val uid = userRepo.currentUser()?.uid ?: return@launch
            userRepo.updateProfile(uid, name.trim(), bio.trim(), photoUrl, themeColor)
                .onSuccess {
                    _message.value = "Profil mis à jour"
                    load()
                }
                .onFailure { _message.value = it.message }
        }
    }

    /** Signale un événement sur un ami, puis recharge les données. */
    fun report(target: UserProfile, type: EventType, description: String) {
        viewModelScope.launch {
            eventRepo.reportEvent(target.uid, type, description)
                .onSuccess {
                    _message.value = "${type.label} signalé pour ${target.displayName}"
                    _dailyLimits.value = eventRepo.myDailyLimits()
                    // Propage le delta dans tous les groupes communs.
                    val uid = myUid
                    if (uid != null) {
                        groupRepo.applyDeltaInCommonGroups(uid, target.uid, type.delta)
                    }
                    load()
                }
                .onFailure { _message.value = it.message }
        }
    }

    /** Affiche un message transitoire venant d'un autre écran (ex. battles). */
    fun showMessage(text: String) { _message.value = text }

    fun clearMessage() { _message.value = null }

    // ── Groupes ───────────────────────────────────────────────────────

    fun createGroup(name: String) {
        viewModelScope.launch {
            groupRepo.createGroup(name)
                .onSuccess {
                    _message.value = "Groupe « ${it.name} » créé !"
                    groupRepo.myGroups().onSuccess { list -> _groups.value = list }
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun inviteToGroup(group: Group, target: UserProfile) {
        viewModelScope.launch {
            groupRepo.inviteToGroup(group, target)
                .onSuccess { _message.value = "${target.displayName.ifBlank { target.email }} invité dans ${group.name}" }
                .onFailure { _message.value = it.message }
        }
    }

    fun acceptGroupInvite(invite: GroupInvite) {
        viewModelScope.launch {
            groupRepo.acceptInvite(invite)
                .onSuccess {
                    _message.value = "Vous avez rejoint « ${invite.groupName} »"
                    _groupInvites.value = _groupInvites.value.filterNot { it.id == invite.id }
                    groupRepo.myGroups().onSuccess { list -> _groups.value = list }
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun declineGroupInvite(invite: GroupInvite) {
        viewModelScope.launch {
            groupRepo.declineInvite(invite)
                .onSuccess { _groupInvites.value = _groupInvites.value.filterNot { it.id == invite.id } }
                .onFailure { _message.value = it.message }
        }
    }

    fun leaveGroup(group: Group) {
        viewModelScope.launch {
            groupRepo.leaveGroup(group)
                .onSuccess {
                    _message.value = "Vous avez quitté « ${group.name} »"
                    _groups.value = _groups.value.filterNot { it.id == group.id }
                    if (_selectedGroup.value?.id == group.id) _selectedGroup.value = null
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun deleteGroup(group: Group) {
        viewModelScope.launch {
            groupRepo.deleteGroup(group)
                .onSuccess {
                    _message.value = "Groupe « ${group.name} » supprimé"
                    _groups.value = _groups.value.filterNot { it.id == group.id }
                    if (_selectedGroup.value?.id == group.id) _selectedGroup.value = null
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun openGroup(group: Group) {
        _selectedGroup.value = group
        _groupLeaderboard.value = emptyList()
        _groupLoading.value = true
        viewModelScope.launch {
            groupRepo.groupLeaderboard(group.id)
                .onSuccess { members ->
                    if (members.isEmpty() && group.memberUids.isNotEmpty()) {
                        // Anciens groupes sans docs membres : on les crée à la volée.
                        groupRepo.initMissingMembers(group)
                        groupRepo.groupLeaderboard(group.id)
                            .onSuccess { _groupLeaderboard.value = it }
                            .onFailure { _groupLeaderboard.value = emptyList() }
                    } else {
                        _groupLeaderboard.value = members
                    }
                }
                .onFailure {
                    _message.value = it.message
                    _groupLeaderboard.value = emptyList()
                }
            _groupLoading.value = false
        }
    }

    fun reportGroupEvent(groupId: String, targetUid: String, type: EventType, description: String) {
        viewModelScope.launch {
            groupRepo.reportGroupEvent(groupId, targetUid, type, description)
                .onSuccess {
                    _message.value = "${type.label} signalé dans le groupe"
                    // Recharge le classement du groupe.
                    groupRepo.groupLeaderboard(groupId)
                        .onSuccess { _groupLeaderboard.value = it }
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun closeGroup() {
        _selectedGroup.value = null
        _groupLeaderboard.value = emptyList()
        _groupLoading.value = false
    }

    fun generateGroupInviteLink(group: Group, onResult: (token: String) -> Unit) {
        viewModelScope.launch {
            groupRepo.generateInviteLink(group)
                .onSuccess { onResult(it) }
                .onFailure { _message.value = it.message }
        }
    }

    fun joinGroupByToken(token: String) {
        viewModelScope.launch {
            groupRepo.joinByToken(token)
                .onSuccess { group ->
                    _message.value = "Vous avez rejoint « ${group.name} »"
                    groupRepo.myGroups().onSuccess { list -> _groups.value = list }
                }
                .onFailure { _message.value = it.message }
        }
    }
}
