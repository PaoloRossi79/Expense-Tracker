package com.paolorossi.expensetracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun `formats whole and fractional pounds`() {
        assertEquals("£12.50", Money.formatPence(1250))
        assertEquals("£0.00", Money.formatPence(0))
        assertEquals("£1.05", Money.formatPence(105))
    }

    @Test
    fun `formats with thousands separators`() {
        assertEquals("£1,234.56", Money.formatPence(123456))
    }

    @Test
    fun `formats negative balances`() {
        assertEquals("-£12.50", Money.formatPence(-1250))
        assertEquals("-£1,234.56", Money.formatPence(-123456))
    }

    @Test
    fun `parses plain decimal input`() {
        assertEquals(1250L, Money.parsePoundsToPence("12.50"))
        assertEquals(1234L, Money.parsePoundsToPence("12.34"))
    }

    @Test
    fun `parses input with pound sign and commas`() {
        assertEquals(123400L, Money.parsePoundsToPence("£1,234"))
        assertEquals(123456L, Money.parsePoundsToPence("£1,234.56"))
    }

    @Test
    fun `rounds extra decimal places to two`() {
        assertEquals(1235L, Money.parsePoundsToPence("12.345"))
    }

    @Test
    fun `rejects blank malformed and negative input`() {
        assertNull(Money.parsePoundsToPence(""))
        assertNull(Money.parsePoundsToPence("   "))
        assertNull(Money.parsePoundsToPence("abc"))
        assertNull(Money.parsePoundsToPence("-1"))
    }

    @Test
    fun `toPlainString pads pence`() {
        assertEquals("12.05", Money.toPlainString(1205))
        assertEquals("0.00", Money.toPlainString(0))
        assertEquals("1234.56", Money.toPlainString(123456))
    }
}
