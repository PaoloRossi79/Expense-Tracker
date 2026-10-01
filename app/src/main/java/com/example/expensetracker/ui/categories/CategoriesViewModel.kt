package com.paolorossi.expensetracker.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paolorossi.expensetracker.data.ServiceLocator
import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.CategoryType
import com.paolorossi.expensetracker.data.repository.CategoryRepository
import com.paolorossi.expensetracker.data.repository.TransactionRepository
import com.paolorossi.expensetracker.domain.CategoryRules
import com.paolorossi.expensetracker.domain.DeletionCheck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val error: String? = null,
)

class CategoriesViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CategoriesUiState())
    val state: StateFlow<CategoriesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories(ServiceLocator.householdId).collect { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
    }

    fun create(
        name: String,
        type: CategoryType,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                categoryRepository.createCategory(
                    ServiceLocator.householdId,
                    Category(name = name.trim(), type = type),
                )
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Create failed") }
            }
        }
    }

    fun rename(
        category: Category,
        name: String,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                categoryRepository.updateCategory(
                    ServiceLocator.householdId,
                    category.copy(name = name.trim()),
                )
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Update failed") }
            }
        }
    }

    fun delete(category: Category) {
        viewModelScope.launch {
            val usage =
                runCatching {
                    transactionRepository.countForCategory(ServiceLocator.householdId, category.id)
                }.getOrDefault(0)

            when (val check = CategoryRules.canDelete(usage)) {
                is DeletionCheck.Blocked ->
                    _state.update {
                        it.copy(
                            error =
                                "Can't delete \"${category.name}\" — ${check.usageCount} " +
                                    "transaction(s) still use it.",
                        )
                    }

                DeletionCheck.Allowed ->
                    runCatching {
                        categoryRepository.deleteCategory(ServiceLocator.householdId, category.id)
                    }.onFailure { error ->
                        _state.update { it.copy(error = error.message ?: "Delete failed") }
                    }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
