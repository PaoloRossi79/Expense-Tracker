package com.paolorossi.expensetracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paolorossi.expensetracker.data.ServiceLocator
import com.paolorossi.expensetracker.data.repository.PendingOccurrence
import com.paolorossi.expensetracker.data.repository.RecurringRepository
import com.paolorossi.expensetracker.data.repository.TransactionRepository
import com.paolorossi.expensetracker.domain.MonthlyTotals
import com.paolorossi.expensetracker.domain.MonthlyTotalsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class HomeUiState(
    val month: YearMonth,
    val totals: MonthlyTotals,
    val pending: List<PendingOccurrence>,
    val loading: Boolean,
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringRepository,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val _state =
        MutableStateFlow(
            HomeUiState(month.value, MonthlyTotals(0, 0), emptyList(), loading = true),
        )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                transactionRepository.observeTransactions(ServiceLocator.householdId),
                recurringRepository.observePendingOccurrences(
                    ServiceLocator.householdId,
                    LocalDate.now(),
                ),
                month,
            ) { transactions, pending, selectedMonth ->
                HomeUiState(
                    month = selectedMonth,
                    totals = MonthlyTotalsCalculator.compute(transactions, selectedMonth),
                    pending = pending,
                    loading = false,
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

    fun confirm(pending: PendingOccurrence) {
        val user = ServiceLocator.authRepository.currentUser ?: return
        viewModelScope.launch {
            runCatching {
                recurringRepository.confirmOccurrence(
                    ServiceLocator.householdId,
                    pending.rule,
                    pending.dueDate,
                    user,
                )
            }
        }
    }

    fun skip(pending: PendingOccurrence) {
        val user = ServiceLocator.authRepository.currentUser ?: return
        viewModelScope.launch {
            runCatching {
                recurringRepository.skipOccurrence(
                    ServiceLocator.householdId,
                    pending.rule,
                    pending.dueDate,
                    user.uid,
                )
            }
        }
    }
}
