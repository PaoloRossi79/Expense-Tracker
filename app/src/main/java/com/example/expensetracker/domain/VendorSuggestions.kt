package com.paolorossi.expensetracker.domain

import com.paolorossi.expensetracker.data.model.Vendor
import java.util.Locale

object VendorSuggestions {
    fun filter(
        vendors: List<Vendor>,
        query: String,
        limit: Int = 8,
    ): List<Vendor> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return vendors.take(limit) // list is already newest first
        val starts = vendors.filter { it.name.lowercase(Locale.ROOT).startsWith(q) }
        val contains =
            vendors.filter {
                val n = it.name.lowercase(Locale.ROOT)
                !n.startsWith(q) && n.contains(q)
            }
        return (starts + contains).take(limit)
    }

    /** Category to auto-select for this vendor, or null to leave the field alone. */
    fun categoryFor(
        vendor: Vendor,
        expenseCategoryIds: Set<String>,
    ): String? = vendor.lastCategoryId?.takeIf { it in expenseCategoryIds }
}
