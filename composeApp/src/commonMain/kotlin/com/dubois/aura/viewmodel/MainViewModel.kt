package com.dubois.aura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dubois.aura.model.AuraEvent
import com.dubois.aura.model.EventType
import com.dubois.aura.model.FriendRequest
import com.dubois.aura.model.RankedUser
import com.dubois.aura.model.UserProfile
import com.dubois.aura.repository.EventRepository
import com.dubois.aura.repository.FriendRepository
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

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            val myUid = userRepo.currentUser()?.uid

            // Classement global -> rangs pour tout le monde.
            val all = userRepo.allUsersRanked()
            val ranked = all.mapIndexed { index, profile ->
                RankedUser(profile, rank = index + 1, isMe = profile.uid == myUid)
            }
            _leaderboard.value = ranked
            _me.value = ranked.firstOrNull { it.isMe }

            // Amis = intersection classement / liste d'amis, dans l'ordre du classement.
            val friendUids = friendRepo.friends().map { it.uid }.toSet()
            _friends.value = ranked.filter { it.profile.uid in friendUids }

            // Demandes d'ami reçues.
            _requests.value = friendRepo.incomingRequests()

            // Événements reçus + delta hebdo.
            if (myUid != null) {
                val recent = eventRepo.recentEvents(myUid)
                _events.value = recent
                val weekAgo = Clock.System.now().toEpochMilliseconds() - 7L * 24 * 60 * 60 * 1000
                _weeklyDelta.value = recent.filter { it.createdAt >= weekAgo }.sumOf { it.delta }
            }
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

    /** Met à jour le profil courant (nom + bio), puis recharge les données. */
    fun updateProfile(name: String, bio: String) {
        viewModelScope.launch {
            val uid = userRepo.currentUser()?.uid ?: return@launch
            userRepo.updateProfile(uid, name.trim(), bio.trim())
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
                    load()
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun clearMessage() { _message.value = null }
}
