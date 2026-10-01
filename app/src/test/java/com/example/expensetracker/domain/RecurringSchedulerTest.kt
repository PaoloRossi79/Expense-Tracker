package com.paolorossi.expensetracker.domain

import com.paolorossi.expensetracker.data.recurring.RecurringScheduler
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurringSchedulerTest {
    private fun at(
        hour: Int,
        minute: Int = 0,
    ) = ZonedDateTime.of(2026, 3, 15, hour, minute, 0, 0, ZoneId.of("UTC"))

    @Test
    fun `before the reminder hour schedules later the same day`() {
        val delay = RecurringScheduler.initialDelayMillis(now = at(8), hour = 9)
        assertEquals(60L * 60 * 1000, delay)
    }

    @Test
    fun `after the reminder hour schedules the next day`() {
        val delay = RecurringScheduler.initialDelayMillis(now = at(10), hour = 9)
        assertEquals(23L * 60 * 60 * 1000, delay)
    }

    @Test
    fun `exactly at the reminder hour schedules the next day`() {
        val delay = RecurringScheduler.initialDelayMillis(now = at(9), hour = 9)
        assertEquals(24L * 60 * 60 * 1000, delay)
    }
}
