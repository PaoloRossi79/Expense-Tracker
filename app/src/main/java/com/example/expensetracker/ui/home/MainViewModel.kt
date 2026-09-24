package com.example.expensetracker.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// TEMPORARY demo data — replaced by the Firestore repository in Phase 1.
// Lets the traffic-light balance logic be exercised end-to-end on screen.
private data class TransactionDemo(
    val vendor: String,
    val amountPence: Long,
    val type: String, // "expense" | "income"
)

private val demoTransactions = listOf(
    TransactionDemo("Salary", 250_000, "income"),
    TransactionDemo("Tesco", 4_500, "expense"),
    TransactionDemo("Council Tax", 18_000, "expense"),
    TransactionDemo("PureGym", 3_000, "expense"),
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(compute(demoTransactions))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private companion object {
        fun compute(transactions: List<TransactionDemo>): MainUiState {
            val inPence = transactions.filter { it.type == "income" }.sumOf { it.amountPence }
            val outPence = transactions.filter { it.type == "expense" }.sumOf { it.amountPence }
            return MainUiState(
                totalInPence = inPence,
                totalOutPence = outPence,
                status = computeBalanceStatus(inPence, outPence),
            )
        }
    }
}