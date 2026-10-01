package com.paolorossi.expensetracker.domain

import com.paolorossi.expensetracker.data.model.Transaction
import com.paolorossi.expensetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TransactionFiltersTest {
    private val d1 = LocalDate.of(2026, 3, 1)
    private val d2 = LocalDate.of(2026, 3, 10)
    private val d3 = LocalDate.of(2026, 3, 20)

    private fun tx(
        id: String,
        type: TransactionType,
        amount: Long,
        date: LocalDate,
        categoryId: String = "c1",
    ) = Transaction(
        id = id,
        type = type,
        amountPence = amount,
        date = date,
        vendor = "v",
        categoryId = categoryId,
        createdByUid = "u",
        createdByName = "n",
    )

    private val expenseSmall = tx("a", TransactionType.EXPENSE, 500, d1, "groceries")
    private val expenseLarge = tx("b", TransactionType.EXPENSE, 2000, d3, "bills")
    private val income = tx("c", TransactionType.INCOME, 1000, d2, "salary")
    private val all = listOf(expenseSmall, expenseLarge, income)

    @Test
    fun `filters by type`() {
        val result = TransactionFilters.apply(all, TransactionFilter(type = TransactionType.EXPENSE))
        assertEquals(setOf("a", "b"), result.map { it.id }.toSet())
    }

    @Test
    fun `filters by category`() {
        val result = TransactionFilters.apply(all, TransactionFilter(categoryId = "bills"))
        assertEquals(listOf("b"), result.map { it.id })
    }

    @Test
    fun `default sort is date descending`() {
        val result = TransactionFilters.apply(all)
        assertEquals(listOf("b", "c", "a"), result.map { it.id })
    }

    @Test
    fun `sorts by date ascending`() {
        val result =
            TransactionFilters.apply(
                all,
                sort = TransactionSort(SortField.DATE, SortDirection.ASC),
            )
        assertEquals(listOf("a", "c", "b"), result.map { it.id })
    }

    @Test
    fun `sorts by amount descending`() {
        val result =
            TransactionFilters.apply(
                all,
                sort = TransactionSort(SortField.AMOUNT, SortDirection.DESC),
            )
        assertEquals(listOf("b", "c", "a"), result.map { it.id })
    }

    @Test
    fun `sorts by amount ascending`() {
        val result =
            TransactionFilters.apply(
                all,
                sort = TransactionSort(SortField.AMOUNT, SortDirection.ASC),
            )
        assertEquals(listOf("a", "c", "b"), result.map { it.id })
    }

    @Test
    fun `applies filter and sort together`() {
        val result =
            TransactionFilters.apply(
                all,
                filter = TransactionFilter(type = TransactionType.EXPENSE),
                sort = TransactionSort(SortField.AMOUNT, SortDirection.DESC),
            )
        assertEquals(listOf("b", "a"), result.map { it.id })
    }
}
