package com.paolorossi.expensetracker.domain

import com.paolorossi.expensetracker.data.model.Transaction
import com.paolorossi.expensetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthlyTotalsTest {
    private fun tx(
        id: String,
        type: TransactionType,
        amount: Long,
        date: LocalDate,
    ) = Transaction(
        id = id,
        type = type,
        amountPence = amount,
        date = date,
        vendor = "v",
        categoryId = "c",
        createdByUid = "u",
        createdByName = "n",
    )

    @Test
    fun `sums money in and out for the month`() {
        val transactions =
            listOf(
                tx("a", TransactionType.EXPENSE, 1500, LocalDate.of(2026, 3, 2)),
                tx("b", TransactionType.EXPENSE, 2500, LocalDate.of(2026, 3, 20)),
                tx("c", TransactionType.INCOME, 100_000, LocalDate.of(2026, 3, 25)),
            )
        val totals = MonthlyTotalsCalculator.compute(transactions, YearMonth.of(2026, 3))
        assertEquals(100_000L, totals.moneyInPence)
        assertEquals(4000L, totals.moneyOutPence)
        assertEquals(96_000L, totals.netPence)
    }

    @Test
    fun `ignores transactions outside the month`() {
        val transactions =
            listOf(
                tx("a", TransactionType.EXPENSE, 1500, LocalDate.of(2026, 2, 28)),
                tx("b", TransactionType.EXPENSE, 2500, LocalDate.of(2026, 4, 1)),
                tx("c", TransactionType.INCOME, 1000, LocalDate.of(2026, 3, 1)),
            )
        val totals = MonthlyTotalsCalculator.compute(transactions, YearMonth.of(2026, 3))
        assertEquals(1000L, totals.moneyInPence)
        assertEquals(0L, totals.moneyOutPence)
    }

    @Test
    fun `empty list yields zero totals`() {
        val totals = MonthlyTotalsCalculator.compute(emptyList(), YearMonth.of(2026, 3))
        assertEquals(0L, totals.moneyInPence)
        assertEquals(0L, totals.moneyOutPence)
        assertEquals(0L, totals.netPence)
    }

    @Test
    fun `net can be negative`() {
        val transactions =
            listOf(
                tx("a", TransactionType.EXPENSE, 5000, LocalDate.of(2026, 3, 2)),
            )
        val totals = MonthlyTotalsCalculator.compute(transactions, YearMonth.of(2026, 3))
        assertEquals(-5000L, totals.netPence)
    }
}
