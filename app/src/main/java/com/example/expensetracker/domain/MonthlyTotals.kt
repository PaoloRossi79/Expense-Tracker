package com.paolorossi.expensetracker.domain

import com.paolorossi.expensetracker.data.model.Transaction
import com.paolorossi.expensetracker.data.model.TransactionType
import java.time.YearMonth

data class MonthlyTotals(
    val moneyInPence: Long,
    val moneyOutPence: Long,
) {
    val netPence: Long get() = moneyInPence - moneyOutPence
}

/**
 * Home-screen monthly running totals (Design/01 §4.2). Pure and unit-testable.
 * A transaction's `date` is already stored as its UTC calendar date.
 */
object MonthlyTotalsCalculator {
    fun compute(
        transactions: List<Transaction>,
        month: YearMonth,
    ): MonthlyTotals {
        var moneyIn = 0L
        var moneyOut = 0L
        for (transaction in transactions) {
            if (YearMonth.from(transaction.date) != month) continue
            when (transaction.type) {
                TransactionType.INCOME -> moneyIn += transaction.amountPence
                TransactionType.EXPENSE -> moneyOut += transaction.amountPence
            }
        }
        return MonthlyTotals(moneyInPence = moneyIn, moneyOutPence = moneyOut)
    }
}
