package com.paolorossi.expensetracker.domain

import com.paolorossi.expensetracker.data.model.Vendor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorSuggestionsTest {
    private val tesco = Vendor(key = "tesco", name = "Tesco")
    private val tescoExtra = Vendor(key = "tesco extra", name = "Tesco Extra")
    private val costa = Vendor(key = "costa coffee", name = "Costa Coffee")
    private val boots = Vendor(key = "boots", name = "Boots")
    private val vendors = listOf(tesco, tescoExtra, costa, boots)

    @Test
    fun `matches starting with the query come first`() {
        val result = VendorSuggestions.filter(vendors, "te")

        assertEquals(listOf("Tesco", "Tesco Extra"), result.map { it.name })
    }

    @Test
    fun `matches containing the query are included`() {
        val result = VendorSuggestions.filter(vendors, "ee")

        assertEquals(listOf("Costa Coffee"), result.map { it.name })
    }

    @Test
    fun `matching is case-insensitive`() {
        val result = VendorSuggestions.filter(vendors, "TESCO")

        assertEquals(listOf("Tesco", "Tesco Extra"), result.map { it.name })
    }

    @Test
    fun `empty query returns the first vendors in list order`() {
        val result = VendorSuggestions.filter(vendors, "")

        assertEquals(vendors, result)
    }

    @Test
    fun `no match returns an empty list`() {
        assertTrue(VendorSuggestions.filter(vendors, "zzz").isEmpty())
    }

    @Test
    fun `empty query returns at most eight vendors`() {
        val twelve = (1..12).map { Vendor(key = "vendor-$it", name = "Vendor $it") }

        val result = VendorSuggestions.filter(twelve, "")

        assertEquals(8, result.size)
        assertEquals(twelve.take(8), result)
    }

    @Test
    fun `categoryFor returns the last category when it is still available`() {
        val vendor = tesco.copy(lastCategoryId = "groceries")

        assertEquals("groceries", VendorSuggestions.categoryFor(vendor, setOf("groceries", "transport")))
    }

    @Test
    fun `categoryFor returns null when there is no last category`() {
        assertNull(VendorSuggestions.categoryFor(tesco, setOf("groceries")))
    }

    @Test
    fun `categoryFor returns null when the last category is not available`() {
        val vendor = tesco.copy(lastCategoryId = "deleted-cat")

        assertNull(VendorSuggestions.categoryFor(vendor, setOf("transport")))
    }
}
