package com.paolorossi.expensetracker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class VendorNameFormatterTest {
    @Test
    fun `formats single word`() {
        assertEquals("Tesco", VendorNameFormatter.toDisplayName("tesco"))
        assertEquals("tesco", VendorNameFormatter.toKey("tesco"))
    }

    @Test
    fun `trims and collapses spaces`() {
        assertEquals("Tesco Extra", VendorNameFormatter.toDisplayName("  tesco   extra "))
        assertEquals("tesco extra", VendorNameFormatter.toKey("  tesco   extra "))
    }

    @Test
    fun `lower-cases then title-cases each word`() {
        assertEquals("Tesco Extra", VendorNameFormatter.toDisplayName("TESCO EXTRA"))
        assertEquals("tesco extra", VendorNameFormatter.toKey("TESCO EXTRA"))
    }

    @Test
    fun `keeps apostrophes`() {
        assertEquals("Sainsbury's", VendorNameFormatter.toDisplayName("SAINSBURY'S"))
        assertEquals("sainsbury's", VendorNameFormatter.toKey("SAINSBURY'S"))
    }

    @Test
    fun `keeps ampersands`() {
        assertEquals("M&s", VendorNameFormatter.toDisplayName("m&s"))
        assertEquals("m&s", VendorNameFormatter.toKey("m&s"))
    }

    @Test
    fun `blank input stays blank`() {
        assertEquals("", VendorNameFormatter.toDisplayName(""))
        assertEquals("", VendorNameFormatter.toKey(""))
    }

    @Test
    fun `slashes are replaced in the key only`() {
        assertEquals("A/b", VendorNameFormatter.toDisplayName("a/b"))
        assertEquals("a_b", VendorNameFormatter.toKey("a/b"))
    }

    @Test
    fun `same vendor in different spellings gives the same key`() {
        assertEquals(VendorNameFormatter.toKey("TESCO"), VendorNameFormatter.toKey("tesco "))
    }
}
