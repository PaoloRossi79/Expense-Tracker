package com.paolorossi.expensetracker.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money helpers. All amounts are integers in pence; formatting to £ happens only
 * in the UI layer (Design/03 conventions). Pure and unit-testable.
 */
object Money {
    /** 123456 -> "£1,234.56"; -1250 -> "-£12.50". */
    fun formatPence(pence: Long): String {
        val negative = pence < 0
        val abs = kotlin.math.abs(pence)
        val pounds = abs / 100
        val remainder = abs % 100
        val formatted = "%,d.%02d".format(pounds, remainder)
        return if (negative) "-£$formatted" else "£$formatted"
    }

    /** 1234 -> "12.34" — for prefilling an editable amount field. */
    fun toPlainString(pence: Long): String {
        val pounds = pence / 100
        val remainder = pence % 100
        return "$pounds.${remainder.toString().padStart(2, '0')}"
    }

    /**
     * Parses user input ("12.34", "£12.34", "1,234") into pence.
     * Returns null for blank, malformed, or negative input. Rounds to 2 dp.
     */
    fun parsePoundsToPence(input: String): Long? {
        val cleaned = input.trim().removePrefix("£").replace(",", "").trim()
        if (cleaned.isEmpty()) return null
        return try {
            val value = BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP)
            if (value.signum() < 0) return null
            value.movePointRight(2).longValueExact()
        } catch (_: NumberFormatException) {
            null
        } catch (_: ArithmeticException) {
            null
        }
    }
}
