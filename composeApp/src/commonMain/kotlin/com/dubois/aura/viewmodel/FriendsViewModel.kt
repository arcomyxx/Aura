package com.dubois.aura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dubois.aura.model.Friend
import com.dubois.aura.model.FriendRequest
import com.dubois.aura.model.UserProfile
import com.dubois.aura.repository.FriendRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FriendsViewModel : ViewModel() {

    private val repo = FriendRepository()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends

    private val _requests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val requests: StateFlow<List<FriendRequest>> = _requests

    private val _searchResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val searchResults: StateFlow<List<UserProfile>> = _searchResults

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /** Recharge amis + demandes reçues. À appeler à l'ouverture de l'écran. */
    fun refresh() {
        viewModelScope.launch {
            _friends.value = repo.friends()
            _requests.value = repo.incomingRequests()
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _loading.value = true
            repo.searchUsers(query)
                .onSuccess { _searchResults.value = it }
                .onFailure { _message.value = it.message }
            _loading.value = false
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun sendRequest(target: UserProfile) {
        viewModelScope.launch {
            repo.sendFriendRequest(target)
                .onSuccess {
                    val who = target.displayName.ifEmpty { target.email }
                    _message.value = "Demande envoyée à $who"
                    _searchResults.value = _searchResults.value.filterNot { it.uid == target.uid }
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun accept(request: FriendRequest) {
        viewModelScope.launch {
            repo.acceptRequest(request)
                .onSuccess {
                    _message.value = "${request.fromName.ifEmpty { request.fromEmail }} est maintenant votre ami"
                    refresh()
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun decline(request: FriendRequest) {
        viewModelScope.launch {
            repo.declineRequest(request)
                .onSuccess { refresh() }
                .onFailure { _message.value = it.message }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
