package com.paolorossi.expensetracker.data.model

import java.time.Instant
import java.time.LocalDate

/**
 * A recurring rule (mortgage, council tax, salary, …), monthly by day-of-month.
 * Occurrences are confirmed by the user from a notification (Design/01 §4.7).
 */
data class RecurringRule(
    val id: String = "",
    val type: TransactionType,
    val reason: String,
    val amountPence: Long,
    val categoryId: String,
    val vendorId: String? = null,
    val dayOfMonth: Int,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val active: Boolean = true,
    val createdByUid: String = "",
    val createdByName: String = "",
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

enum class OccurrenceStatus { CONFIRMED, SKIPPED }

/**
 * A resolved occurrence of a [RecurringRule]. The id is the occurrence date as
 * `yyyyMMdd`, which makes confirmation idempotent across devices.
 */
data class Occurrence(
    val id: String,
    val status: OccurrenceStatus,
    val resolvedByUid: String,
    val resolvedAt: Instant? = null,
    val transactionId: String? = null,
)
