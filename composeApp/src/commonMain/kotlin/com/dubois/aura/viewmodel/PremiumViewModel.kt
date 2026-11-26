package com.dubois.aura.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dubois.aura.repository.PremiumRepository
import com.dubois.aura.repository.PremiumStatus
import com.revenuecat.purchases.kmp.Offerings
import com.revenuecat.purchases.kmp.Package
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PurchaseState {
    data object Idle : PurchaseState()
    data object Loading : PurchaseState()
    data object Success : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}

class PremiumViewModel : ViewModel() {

    private val repo = PremiumRepository()

    private val _premiumStatus = MutableStateFlow<PremiumStatus>(PremiumStatus.Unknown)
    val premiumStatus: StateFlow<PremiumStatus> = _premiumStatus.asStateFlow()

    private val _offerings = MutableStateFlow<Offerings?>(null)
    val offerings: StateFlow<Offerings?> = _offerings.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.premiumStatus().collect { _premiumStatus.value = it }
        }
        loadOfferings()
    }

    private fun loadOfferings() {
        viewModelScope.launch {
            repo.getOfferings().onSuccess { _offerings.value = it }
        }
    }

    fun purchase(pkg: Package) {
        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Loading
            repo.purchase(pkg)
                .onSuccess { _purchaseState.value = PurchaseState.Success }
                .onFailure { _purchaseState.value = PurchaseState.Error(it.message ?: "Erreur inconnue") }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _purchaseState.value = PurchaseState.Loading
            repo.restorePurchases()
                .onSuccess { _purchaseState.value = PurchaseState.Success }
                .onFailure { _purchaseState.value = PurchaseState.Error(it.message ?: "Erreur inconnue") }
        }
    }

    fun clearPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }
}
