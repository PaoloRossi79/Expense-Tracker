package com.paolorossi.expensetracker.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateFormatsTest {
    @Test
    fun `formats dates as day month year`() {
        assertEquals("05-03-2026", LocalDate.of(2026, 3, 5).toUkDate())
        assertEquals("22-10-2026", LocalDate.of(2026, 10, 22).toUkDate())
    }

    @Test
    fun `formats transaction dates as day and weekday`() {
        assertEquals("22 - Thursday", LocalDate.of(2026, 10, 22).toTransactionDate())
        assertEquals("5 - Thursday", LocalDate.of(2026, 3, 5).toTransactionDate())
    }
}
