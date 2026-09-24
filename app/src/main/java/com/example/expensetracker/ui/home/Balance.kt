package com.example.expensetracker.ui.home

import kotlin.math.abs

/**
 * Shared, unit-testable business logic for the main screen balance indicator.
 *
 * Amounts are stored in minor units (pence) as integers (see Design/03 conventions),
 * so all math here is integer arithmetic — no floating-point rounding.
 */

/** Traffic-light status for a period's totals. */
private const val THRESHOLD_PENCE = 30_000L // £300

fun computeBalanceStatus(inPence: Long, outPence: Long): BalanceStatus = when {
    (outPence - inPence) > THRESHOLD_PENCE -> BalanceStatus.RED
    (inPence - outPence) > THRESHOLD_PENCE -> BalanceStatus.GREEN
    else -> BalanceStatus.AMBER
}

fun formatPence(pence: Long): String {
    val negative = pence < 0
    val abs = abs(pence)
    val pounds = abs / 100
    val p = abs % 100
    val formatted = "%,d.%02d".format(pounds, p)
    return if (negative) "-£$formatted" else "£$formatted"
}