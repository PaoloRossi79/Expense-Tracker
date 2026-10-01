package com.paolorossi.expensetracker.domain

import com.paolorossi.expensetracker.data.model.Transaction
import com.paolorossi.expensetracker.data.model.TransactionType

enum class SortField { DATE, AMOUNT }

enum class SortDirection { ASC, DESC }

/** null [type]/[categoryId] means "no restriction" (Both / All categories). */
data class TransactionFilter(
    val type: TransactionType? = null,
    val categoryId: String? = null,
)

data class TransactionSort(
    val field: SortField = SortField.DATE,
    val direction: SortDirection = SortDirection.DESC,
)

/**
 * Filtering and sorting for the monthly transactions list (Design/01 §4.5).
 * Isolated and unit-testable; not embedded in composables.
 */
object TransactionFilters {
    fun apply(
        transactions: List<Transaction>,
        filter: TransactionFilter = TransactionFilter(),
        sort: TransactionSort = TransactionSort(),
    ): List<Transaction> {
        val filtered =
            transactions.filter { transaction ->
                (filter.type == null || transaction.type == filter.type) &&
                    (filter.categoryId == null || transaction.categoryId == filter.categoryId)
            }

        val comparator =
            when (sort.field) {
                SortField.DATE -> compareBy<Transaction> { it.date }.thenBy { it.id }
                SortField.AMOUNT -> compareBy<Transaction> { it.amountPence }.thenBy { it.id }
            }

        return if (sort.direction == SortDirection.DESC) {
            filtered.sortedWith(comparator.reversed())
        } else {
            filtered.sortedWith(comparator)
        }
    }
}
