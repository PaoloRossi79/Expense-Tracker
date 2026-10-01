package com.paolorossi.expensetracker.domain

import com.paolorossi.expensetracker.data.model.RecurringRule
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Recurring-schedule logic (Design/01 §4.7, Design/03 "Recurring transactions").
 * Monthly by day-of-month, clamped to the last day of shorter months
 * (day 31 → 28/29 in February). Pure and unit-testable — no Android, no Firestore.
 */
object RecurringSchedule {
    private val OCCURRENCE_FORMAT: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE

    /** `yyyyMMdd` — also the occurrence document id and part of the transaction id. */
    fun occurrenceId(date: LocalDate): String = date.format(OCCURRENCE_FORMAT)

    /** Deterministic, idempotent transaction id: `rec_{ruleId}_{yyyyMMdd}`. */
    fun recurringTransactionId(
        ruleId: String,
        date: LocalDate,
    ): String = "rec_${ruleId}_${occurrenceId(date)}"

    /** Day-of-month clamped to the month's length: (Feb, 31) -> 28/29. */
    fun clampToMonth(
        yearMonth: YearMonth,
        dayOfMonth: Int,
    ): LocalDate = yearMonth.atDay(dayOfMonth.coerceIn(1, yearMonth.lengthOfMonth()))

    /** The rule's due date within a month, or null if it falls outside start/end. */
    fun dueDateInMonth(
        rule: RecurringRule,
        yearMonth: YearMonth,
    ): LocalDate? {
        val candidate = clampToMonth(yearMonth, rule.dayOfMonth)
        if (candidate.isBefore(rule.startDate)) return null
        if (rule.endDate != null && candidate.isAfter(rule.endDate)) return null
        return candidate
    }

    /**
     * Every due date from the rule's start up to [today] (inclusive), oldest first,
     * bounded to [maxMonthsBack] months. Returns empty for a paused rule.
     */
    fun dueDatesUpTo(
        rule: RecurringRule,
        today: LocalDate,
        maxMonthsBack: Int = 24,
    ): List<LocalDate> {
        if (!rule.active) return emptyList()
        val earliest = YearMonth.from(rule.startDate)
        val boundedToday = if (rule.endDate != null && rule.endDate.isBefore(today)) rule.endDate else today
        val latest = YearMonth.from(boundedToday)
        if (latest.isBefore(earliest)) return emptyList()

        val floor = YearMonth.from(today).minusMonths(maxMonthsBack.toLong())
        var month = if (floor.isAfter(earliest)) floor else earliest

        val dates = mutableListOf<LocalDate>()
        while (!month.isAfter(latest)) {
            dueDateInMonth(rule, month)?.let { if (!it.isAfter(today)) dates.add(it) }
            month = month.plusMonths(1)
        }
        return dates
    }

    /** Unresolved due dates (no occurrence doc) — these are "pending" (§4.7). */
    fun pendingDueDates(
        rule: RecurringRule,
        today: LocalDate,
        resolvedDates: Set<LocalDate>,
        maxMonthsBack: Int = 24,
    ): List<LocalDate> = dueDatesUpTo(rule, today, maxMonthsBack).filterNot { it in resolvedDates }

    /** The next due date on/after [today], or null if the rule has ended. */
    fun nextDueDate(
        rule: RecurringRule,
        today: LocalDate,
    ): LocalDate? {
        if (!rule.active) return null
        val startMonth = YearMonth.from(rule.startDate)
        var month = if (YearMonth.from(today).isAfter(startMonth)) YearMonth.from(today) else startMonth
        repeat(24) {
            val due = dueDateInMonth(rule, month)
            if (due != null && !due.isBefore(today)) return due
            month = month.plusMonths(1)
        }
        return null
    }
}
