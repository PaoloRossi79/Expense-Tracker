package com.paolorossi.expensetracker.navigation

import com.paolorossi.expensetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {
    @Test
    fun `edit builds a query route for an expense`() {
        assertEquals(
            "add/expense?transactionId=tx-1",
            Routes.edit("tx-1", TransactionType.EXPENSE),
        )
    }

    @Test
    fun `edit builds a query route for income`() {
        assertEquals(
            "add/income?transactionId=tx-2",
            Routes.edit("tx-2", TransactionType.INCOME),
        )
    }

    @Test
    fun `confirmRecurring builds a query route`() {
        assertEquals(
            "add/expense?ruleId=r-1&dueDate=2026-01-31",
            Routes.confirmRecurring("r-1", "2026-01-31"),
        )
    }

    @Test
    fun `add base routes match their destination patterns`() {
        assertTrue(Routes.ADD_EXPENSE_ROUTE.startsWith("${Routes.ADD_EXPENSE}?"))
        assertTrue(Routes.ADD_INCOME_ROUTE.startsWith("${Routes.ADD_INCOME}?"))
    }
}
