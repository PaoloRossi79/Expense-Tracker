package com.paolorossi.expensetracker.domain

import com.paolorossi.expensetracker.data.model.Vendor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VendorMatcherTest {
    private val tesco = Vendor(id = "1", name = "Tesco", defaultCategoryId = "groceries")
    private val vodafone =
        Vendor(
            id = "2",
            name = "Vodafone",
            defaultCategoryId = "comms",
            matchAliases = listOf("Vodafone Ltd", "VF"),
        )
    private val vendors = listOf(tesco, vodafone)

    @Test
    fun `exact name match wins`() {
        assertEquals(tesco, VendorMatcher.bestMatch("Tesco", vendors)?.vendor)
    }

    @Test
    fun `normalises store numbers and punctuation`() {
        assertEquals(tesco, VendorMatcher.bestMatch("TESCO STORES 2903", vendors)?.vendor)
        assertEquals(tesco, VendorMatcher.bestMatch("Tesco.", vendors)?.vendor)
    }

    @Test
    fun `matches aliases`() {
        assertEquals(vodafone, VendorMatcher.bestMatch("VODAFONE LTD", vendors)?.vendor)
        assertEquals(vodafone, VendorMatcher.bestMatch("VF", vendors)?.vendor)
    }

    @Test
    fun `matches a needle contained in a known vendor name`() {
        assertEquals(vodafone, VendorMatcher.bestMatch("Vodafone Broadband", vendors)?.vendor)
    }

    @Test
    fun `returns null for no match`() {
        assertNull(VendorMatcher.bestMatch("Some Random Shop", vendors))
    }

    @Test
    fun `returns null for blank input`() {
        assertNull(VendorMatcher.bestMatch("", vendors))
        assertNull(VendorMatcher.bestMatch("   ", vendors))
    }

    @Test
    fun `normalise strips punctuation and store numbers`() {
        assertEquals("TESCO STORES", VendorMatcher.normalise("Tesco Stores 12345 !! "))
        assertEquals("TESCO", VendorMatcher.normalise("  TESCO.  "))
    }
}
