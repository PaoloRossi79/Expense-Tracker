package com.paolorossi.expensetracker.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paolorossi.expensetracker.data.ServiceLocator
import com.paolorossi.expensetracker.data.model.Household
import com.paolorossi.expensetracker.data.repository.HouseholdRepository
import com.paolorossi.expensetracker.domain.EmailValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MembersUiState(
    val household: Household? = null,
    val email: String = "",
    val message: String? = null,
    val error: String? = null,
)

class AddMemberViewModel(
    private val householdRepository: HouseholdRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MembersUiState())
    val state: StateFlow<MembersUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            householdRepository.observeHousehold(ServiceLocator.householdId).collect { household ->
                _state.update { it.copy(household = household) }
            }
        }
    }

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, error = null, message = null) }
    }

    fun add() {
        val email = _state.value.email.trim()
        if (!EmailValidator.isValid(email)) {
            _state.update { it.copy(error = "Enter a valid email address") }
            return
        }
        viewModelScope.launch {
            val added =
                runCatching {
                    householdRepository.addMember(ServiceLocator.householdId, email)
                }.getOrDefault(false)

            _state.update {
                if (added) {
                    it.copy(email = "", message = "Added $email to the household")
                } else {
                    it.copy(error = "$email is already a member")
                }
            }
        }
    }
}
