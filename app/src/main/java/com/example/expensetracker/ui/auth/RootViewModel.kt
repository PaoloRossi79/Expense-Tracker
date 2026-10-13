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

    /** Human-readable outcome of the last diagnostics upload attempt. */
    private val _diagnosticsStatus = MutableStateFlow<String?>(null)
    val diagnosticsStatus: StateFlow<String?> = _diagnosticsStatus.asStateFlow()

    /** Stable per-install id, surfaced in the UI so a user can relay it. */
    val installId: String? get() = ServiceLocator.diagnosticsInstallId

    private var unlocked = false
    private var lastUser: AuthUser? = null
    private var lastHousehold: Household? = null
    private var lastError: String? = null
    private var lastFailure: Throwable? = null
    private var diagnosticsInFlight = false

    init {
        if (!ServiceLocator.isConfigured) {
            _state.value = RootState.NotConfigured
        } else {
            observeAuth()
            viewModelScope.launch { ServiceLocator.diagnosticsReporter?.flushPending() }
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
            lastFailure = result.exceptionOrNull()
            lastError = lastFailure?.message ?: lastFailure?.javaClass?.simpleName
            if (result.isFailure) {
                _state.value = RootState.SignedOut(lastError)
                // Cancelling the account picker is not a failure worth reporting.
                if (!isUserCancellation(lastFailure)) {
                    reportDiagnostics(trigger = "sign_in_failure", note = null)
                }
            }
        }
    }

    /** Uploads a diagnostic report from this device (manual or automatic). */
    fun sendDiagnostics(note: String?) {
        viewModelScope.launch { reportDiagnostics(trigger = "manual", note = note) }
    }

    private suspend fun reportDiagnostics(
        trigger: String,
        note: String?,
    ) {
        if (diagnosticsInFlight) return
        diagnosticsInFlight = true
        try {
            val reporter = ServiceLocator.diagnosticsReporter
            if (reporter == null) {
                _diagnosticsStatus.value = "Diagnostics unavailable (Firebase not configured)"
                return
            }
            _diagnosticsStatus.value = "Sending diagnostics…"
            _diagnosticsStatus.value =
                reporter.send(lastFailure, trigger, note).fold(
                    onSuccess = { id -> "Diagnostics sent · id …${id.takeLast(12)}" },
                    onFailure = { t ->
                        "Diagnostics failed: ${t.message ?: t.javaClass.simpleName}"
                    },
                )
        } finally {
            diagnosticsInFlight = false
        }
    }

    /** A cancelled account picker surfaces as an ordinary failure result. */
    private fun isUserCancellation(throwable: Throwable?): Boolean =
        throwable?.javaClass?.name?.contains("Cancel", ignoreCase = true) == true

    /** Called by the UI after [com.paolorossi.expensetracker.data.auth.BiometricGate] succeeds. */
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
