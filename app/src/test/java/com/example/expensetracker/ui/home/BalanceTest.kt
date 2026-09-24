package com.example.expensetracker.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceTest {

    // --- Traffic-light thresholds (threshold = £300 = 30_000 pence) ---

    @Test
    fun `green when income exceeds outgoings by more than threshold`() {
        val status = computeBalanceStatus(inPence = 100_000, outPence = 50_000)
        assertEquals(BalanceStatus.GREEN, status)
    }

    @Test
    fun `red when outgoings exceed income by more than threshold`() {
        val status = computeBalanceStatus(inPence = 100_000, outPence = 150_000)
        assertEquals(BalanceStatus.RED, status)
    }

    @Test
    fun `amber when within threshold either way`() {
        // Out exceeds In by £299 — just inside the amber band.
        assertEquals(BalanceStatus.AMBER, computeBalanceStatus(inPence = 100_000, outPence = 129_900))
        // In exceeds Out by £299 — just inside the amber band.
        assertEquals(BalanceStatus.AMBER, computeBalanceStatus(inPence = 129_900, outPence = 100_000))
    }

    @Test
    fun `exactly at threshold is amber, not red or green`() {
        // Out - In = exactly 30_000 pence is NOT > 30_000, so amber.
        assertEquals(BalanceStatus.AMBER, computeBalanceStatus(inPence = 100_000, outPence = 130_000))
        // In - Out = exactly 30_000 pence is NOT > 30_000, so amber.
        assertEquals(BalanceStatus.AMBER, computeBalanceStatus(inPence = 130_000, outPence = 100_000))
    }

    @Test
    fun `just past threshold flips the status`() {
        // Out - In = 30_001 pence — one penny over, now red.
        assertEquals(BalanceStatus.RED, computeBalanceStatus(inPence = 100_000, outPence = 130_001))
        // In - Out = 30_001 pence — one penny over, now green.
        assertEquals(BalanceStatus.GREEN, computeBalanceStatus(inPence = 130_001, outPence = 100_000))
    }

    @Test
    fun `balanced month is amber`() {
        assertEquals(BalanceStatus.AMBER, computeBalanceStatus(inPence = 50_000, outPence = 50_000))
    }

    // --- Formatting ---

    @Test
    fun `formatPence formats whole pounds`() {
        assertEquals("£12.50", formatPence(1250))
    }

    @Test
    fun `formatPence formats zero`() {
        assertEquals("£0.00", formatPence(0))
    }

    @Test
    fun `formatPence pads single-digit pence`() {
        assertEquals("£1.05", formatPence(105))
    }

    @Test
    fun `formatPence uses thousands separators`() {
        assertEquals("£1,234.56", formatPence(123456))
    }

    @Test
    fun `formatPence formats negative balances`() {
        assertEquals("-£12.50", formatPence(-1250))
    }

    @Test
    fun `formatPence handles large negative value`() {
        assertEquals("-£1,234.56", formatPence(-123456))
    }

    @Test
    fun `formatPence does not round — exact pence only`() {
        // 1234 pence = £12.34 exactly, no rounding to £12.35.
        assertEquals("£12.34", formatPence(1234))
    }

    // --- End-to-end: a MainUiState built from the same aggregation the ViewModel uses ---

    @Test
    fun `MainUiState reflects the traffic-light status`() {
        val state = MainUiState(
            totalInPence = 100_000,
            totalOutPence = 130_001,
            status = computeBalanceStatus(100_000, 130_001),
        )
        assertEquals(BalanceStatus.RED, state.status)
        assertEquals(-30_001, state.balancePence)
        assertTrue(state.statusLabel.contains("Over"))
    }
}