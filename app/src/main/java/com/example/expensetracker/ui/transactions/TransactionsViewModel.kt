package com.paolorossi.expensetracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paolorossi.expensetracker.data.ServiceLocator
import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.Transaction
import com.paolorossi.expensetracker.data.model.TransactionType
import com.paolorossi.expensetracker.data.repository.CategoryRepository
import com.paolorossi.expensetracker.data.repository.TransactionRepository
import com.paolorossi.expensetracker.domain.SortDirection
import com.paolorossi.expensetracker.domain.SortField
import com.paolorossi.expensetracker.domain.TransactionFilter
import com.paolorossi.expensetracker.domain.TransactionFilters
import com.paolorossi.expensetracker.domain.TransactionSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

data class TransactionsUiState(
    val month: YearMonth = YearMonth.now(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filter: TransactionFilter = TransactionFilter(),
    val sort: TransactionSort = TransactionSort(),
    val currentUserUid: String? = null,
    val deleteCandidate: Transaction? = null,
    val error: String? = null,
)

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val currentUserUidProvider: () -> String? = {
        ServiceLocator.authRepository.currentUser?.uid
    },
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val filter = MutableStateFlow(TransactionFilter())
    private val sort = MutableStateFlow(TransactionSort())
    private val deleteCandidate = MutableStateFlow<Transaction?>(null)
    private val _state = MutableStateFlow(TransactionsUiState())
    val state: StateFlow<TransactionsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val data =
                combine(
                    transactionRepository.observeTransactions(ServiceLocator.householdId),
                    categoryRepository.observeCategories(ServiceLocator.householdId),
                ) { transactions, categories -> transactions to categories }

            combine(data, month, filter, sort, deleteCandidate) {
                    (transactions, categories),
                    selectedMonth,
                    selectedFilter,
                    selectedSort,
                    candidate,
                ->
                val monthly = transactions.filter { YearMonth.from(it.date) == selectedMonth }
                TransactionsUiState(
                    month = selectedMonth,
                    transactions = TransactionFilters.apply(monthly, selectedFilter, selectedSort),
                    categories = categories,
                    filter = selectedFilter,
                    sort = selectedSort,
                    currentUserUid = currentUserUidProvider(),
                    deleteCandidate = candidate,
                )
            }.collect { _state.value = it }
        }
    }

    fun showPreviousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun showNextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun setTypeFilter(type: TransactionType?) {
        filter.update { it.copy(type = type) }
    }

    fun setCategoryFilter(categoryId: String?) {
        filter.update { it.copy(categoryId = categoryId) }
    }

    fun toggleSortField(field: SortField) {
        sort.update {
            if (it.field == field) it.copy(direction = it.direction.opposite()) else TransactionSort(field, SortDirection.DESC)
        }
    }

    fun requestDelete(transaction: Transaction) {
        deleteCandidate.value = transaction
    }

    fun cancelDelete() {
        deleteCandidate.value = null
    }

    fun confirmDelete() {
        val candidate = deleteCandidate.value ?: return
        viewModelScope.launch {
            runCatching {
                transactionRepository.deleteTransaction(ServiceLocator.householdId, candidate.id)
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Delete failed") }
            }
            deleteCandidate.value = null
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

private fun SortDirection.opposite(): SortDirection = if (this == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
