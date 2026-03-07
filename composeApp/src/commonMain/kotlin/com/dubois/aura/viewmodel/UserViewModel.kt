package com.dubois.aura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dubois.aura.model.UserProfile
import com.dubois.aura.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {

    private val repo = UserRepository()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            repo.register(email, password, name)
                .onSuccess { _profile.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            repo.login(email, password)
                .onSuccess { _profile.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun updateProfile(name: String, bio: String) {
        viewModelScope.launch {
            val uid = repo.currentUser()?.uid ?: return@launch
            repo.updateProfile(uid, name, bio)
                .onSuccess { _profile.value = _profile.value?.copy(displayName = name, bio = bio) }
                .onFailure { _error.value = it.message }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _profile.value = null
        }
    }
}