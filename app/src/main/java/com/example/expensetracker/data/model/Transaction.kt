package com.paolorossi.expensetracker.data.model

import java.time.Instant
import java.time.LocalDate

/** Whether a record is money out ([EXPENSE]) or money in ([INCOME]). */
enum class TransactionType { EXPENSE, INCOME }

/**
 * A single money-in or money-out record.
 *
 * [amountPence] is stored in minor units (pence) as an integer — never a float —
 * to avoid rounding errors (Design/03 conventions).
 */
data class Transaction(
    val id: String = "",
    val type: TransactionType,
    val amountPence: Long,
    val date: LocalDate,
    val vendor: String,
    val vendorId: String? = null,
    val categoryId: String,
    val isRecurringInstance: Boolean = false,
    val recurringRuleId: String? = null,
    val createdByUid: String,
    val createdByName: String,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)
