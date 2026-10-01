package com.paolorossi.expensetracker.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paolorossi.expensetracker.data.ServiceLocator
import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.RecurringRule
import com.paolorossi.expensetracker.data.model.Vendor
import com.paolorossi.expensetracker.data.repository.CategoryRepository
import com.paolorossi.expensetracker.data.repository.RecurringRepository
import com.paolorossi.expensetracker.data.repository.VendorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecurringRulesUiState(
    val rules: List<RecurringRule> = emptyList(),
    val categories: List<Category> = emptyList(),
    val vendors: List<Vendor> = emptyList(),
    val error: String? = null,
)

class RecurringRulesViewModel(
    private val recurringRepository: RecurringRepository,
    private val categoryRepository: CategoryRepository,
    private val vendorRepository: VendorRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(RecurringRulesUiState())
    val state: StateFlow<RecurringRulesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val base =
                combine(
                    recurringRepository.observeRules(ServiceLocator.householdId),
                    categoryRepository.observeCategories(ServiceLocator.householdId),
                ) { rules, categories -> rules to categories }

            combine(base, vendorRepository.observeVendors(ServiceLocator.householdId)) {
                    (rules, categories),
                    vendors,
                ->
                RecurringRulesUiState(rules = rules, categories = categories, vendors = vendors)
            }.collect { _state.value = it }
        }
    }

    fun save(rule: RecurringRule) {
        if (rule.reason.isBlank()) {
            _state.update { it.copy(error = "Enter a reason") }
            return
        }
        if (rule.dayOfMonth !in 1..31) {
            _state.update { it.copy(error = "Day of month must be between 1 and 31") }
            return
        }
        viewModelScope.launch {
            runCatching {
                if (rule.id.isBlank()) {
                    recurringRepository.createRule(ServiceLocator.householdId, rule)
                } else {
                    recurringRepository.updateRule(ServiceLocator.householdId, rule)
                }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Save failed") }
            }
        }
    }

    fun setActive(
        rule: RecurringRule,
        active: Boolean,
    ) {
        viewModelScope.launch {
            runCatching {
                recurringRepository.setActive(ServiceLocator.householdId, rule.id, active)
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Update failed") }
            }
        }
    }

    fun delete(rule: RecurringRule) {
        viewModelScope.launch {
            runCatching {
                recurringRepository.deleteRule(ServiceLocator.householdId, rule.id)
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Delete failed") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
