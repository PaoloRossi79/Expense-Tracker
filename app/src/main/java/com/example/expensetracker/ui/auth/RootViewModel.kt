package com.paolorossi.expensetracker.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paolorossi.expensetracker.data.ServiceLocator
import com.paolorossi.expensetracker.data.auth.AuthUser
import com.paolorossi.expensetracker.data.model.Household
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** App-level gate state: setup → sign-in → membership → biometric unlock → ready. */
sealed interface RootState {
    data object Loading : RootState

    data object NotConfigured : RootState

    data class SignedOut(val error: String? = null) : RootState

    data class NotAMember(val email: String) : RootState

    data object Locked : RootState

    data class Ready(val user: AuthUser, val household: Household) : RootState
}

class RootViewModel : ViewModel() {
    private val _state = MutableStateFlow<RootState>(RootState.Loading)
    val state: StateFlow<RootState> = _state.asStateFlow()

    private var unlocked = false
    private var lastUser: AuthUser? = null
    private var lastHousehold: Household? = null
    private var lastError: String? = null

    init {
        if (!ServiceLocator.isConfigured) {
            _state.value = RootState.NotConfigured
        } else {
            observeAuth()
        }
    }

    private fun observeAuth() {
        viewModelScope.launch {
            ServiceLocator.authRepository.observeAuthState().collectLatest { user ->
                lastUser = user
                if (user == null) {
                    lastHousehold = null
                    unlocked = false
                    _state.value = RootState.SignedOut(lastError)
                    return@collectLatest
                }
                ServiceLocator.householdRepository.observeHousehold(ServiceLocator.householdId)
                    .collectLatest { household ->
                        lastHousehold = household
                        recompute()
                    }
            }
        }
    }

    private fun recompute() {
        val user = lastUser
        val household = lastHousehold
        _state.value =
            when {
                user == null -> RootState.SignedOut(lastError)
                household == null -> RootState.Loading
                household.memberEmails.none { it.equals(user.email, ignoreCase = true) } ->
                    RootState.NotAMember(user.email)

                unlocked -> RootState.Ready(user, household)
                else -> RootState.Locked
            }
    }

    fun signIn(activity: Activity) {
        viewModelScope.launch {
            _state.value = RootState.Loading
            val result = ServiceLocator.authRepository.signInWithGoogle(activity)
            lastError = result.exceptionOrNull()?.message
            if (result.isFailure) _state.value = RootState.SignedOut(lastError)
        }
    }

    /** Called by the UI after [BiometricGate] succeeds. */
    fun onUnlocked() {
        unlocked = true
        recompute()
    }

    fun refreshMembership() {
        recompute()
    }

    fun signOut() {
        unlocked = false
        lastError = null
        ServiceLocator.authRepository.signOut()
    }
}
