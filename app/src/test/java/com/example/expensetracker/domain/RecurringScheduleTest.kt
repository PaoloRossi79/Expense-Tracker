package com.paolorossi.expensetracker.domain

import com.paolorossi.expensetracker.data.model.RecurringRule
import com.paolorossi.expensetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RecurringScheduleTest {
    private fun rule(
        dayOfMonth: Int,
        startDate: LocalDate = LocalDate.of(2026, 1, 1),
        endDate: LocalDate? = null,
        active: Boolean = true,
        id: String = "rule1",
    ) = RecurringRule(
        id = id,
        type = TransactionType.EXPENSE,
        reason = "Mortgage",
        amountPence = 125_000,
        categoryId = "bills",
        dayOfMonth = dayOfMonth,
        startDate = startDate,
        endDate = endDate,
        active = active,
    )

    @Test
    fun `clamps day 31 to the last day of February`() {
        assertEquals(LocalDate.of(2026, 2, 28), RecurringSchedule.clampToMonth(YearMonth.of(2026, 2), 31))
    }

    @Test
    fun `clamps to leap-year February 29`() {
        assertEquals(LocalDate.of(2024, 2, 29), RecurringSchedule.clampToMonth(YearMonth.of(2024, 2), 31))
    }

    @Test
    fun `clamps day 29 and 30 in February`() {
        assertEquals(LocalDate.of(2026, 2, 28), RecurringSchedule.clampToMonth(YearMonth.of(2026, 2), 29))
        assertEquals(LocalDate.of(2026, 2, 28), RecurringSchedule.clampToMonth(YearMonth.of(2026, 2), 30))
    }

    @Test
    fun `occurrence id is yyyyMMdd`() {
        assertEquals("20260131", RecurringSchedule.occurrenceId(LocalDate.of(2026, 1, 31)))
    }

    @Test
    fun `recurring transaction id is deterministic`() {
        assertEquals(
            "rec_rule1_20260131",
            RecurringSchedule.recurringTransactionId("rule1", LocalDate.of(2026, 1, 31)),
        )
    }

    @Test
    fun `due date in month respects start and end bounds`() {
        val r = rule(dayOfMonth = 15, startDate = LocalDate.of(2026, 3, 1))
        assertNull(RecurringSchedule.dueDateInMonth(r, YearMonth.of(2026, 2)))
        assertEquals(LocalDate.of(2026, 3, 15), RecurringSchedule.dueDateInMonth(r, YearMonth.of(2026, 3)))

        val ended = rule(dayOfMonth = 15, startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 3, 31))
        assertNull(RecurringSchedule.dueDateInMonth(ended, YearMonth.of(2026, 4)))
    }

    @Test
    fun `dueDatesUpTo lists each month from start to today`() {
        val r = rule(dayOfMonth = 10, startDate = LocalDate.of(2026, 1, 1))
        val dues = RecurringSchedule.dueDatesUpTo(r, today = LocalDate.of(2026, 3, 15))
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 3, 10),
            ),
            dues,
        )
    }

    @Test
    fun `dueDatesUpTo excludes a due date later in the current month`() {
        val r = rule(dayOfMonth = 20, startDate = LocalDate.of(2026, 1, 1))
        val dues = RecurringSchedule.dueDatesUpTo(r, today = LocalDate.of(2026, 3, 15))
        assertTrue(dues.none { it == LocalDate.of(2026, 3, 20) })
    }

    @Test
    fun `paused rule has no due dates`() {
        val r = rule(dayOfMonth = 5, active = false)
        assertTrue(RecurringSchedule.dueDatesUpTo(r, LocalDate.of(2026, 3, 15)).isEmpty())
        assertNull(RecurringSchedule.nextDueDate(r, LocalDate.of(2026, 3, 15)))
    }

    @Test
    fun `pendingDueDates excludes resolved occurrences`() {
        val r = rule(dayOfMonth = 10, startDate = LocalDate.of(2026, 1, 1))
        val resolved = setOf(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 3, 10))
        val pending = RecurringSchedule.pendingDueDates(r, LocalDate.of(2026, 3, 15), resolved)
        assertEquals(listOf(LocalDate.of(2026, 2, 10)), pending)
    }

    @Test
    fun `nextDueDate returns today when due today`() {
        val r = rule(dayOfMonth = 15, startDate = LocalDate.of(2026, 1, 1))
        assertEquals(LocalDate.of(2026, 3, 15), RecurringSchedule.nextDueDate(r, LocalDate.of(2026, 3, 15)))
    }

    @Test
    fun `nextDueDate rolls to next month after the due day`() {
        val r = rule(dayOfMonth = 10, startDate = LocalDate.of(2026, 1, 1))
        assertEquals(LocalDate.of(2026, 4, 10), RecurringSchedule.nextDueDate(r, LocalDate.of(2026, 3, 15)))
    }
}
