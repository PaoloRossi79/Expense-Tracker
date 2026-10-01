package com.paolorossi.expensetracker.domain

import com.paolorossi.expensetracker.data.model.Vendor

data class VendorMatch(val vendor: Vendor, val matchedAlias: String)

/**
 * Vendor auto-allocation (Design/01 §4.8, Design/03 "Vendor matching").
 *
 * Normalises the typed string, then matches: exact vendor name → exact alias →
 * "contains" substring (longest match wins). "TESCO STORES 2903" matches "Tesco".
 * Kept as one isolated, unit-testable function — not scattered in UI code.
 */
object VendorMatcher {
    private val STORE_NUMBER = Regex("""\b\d{2,}\b""")
    private val NON_ALNUM = Regex("""[^A-Z0-9 ]""")
    private val WHITESPACE = Regex("""\s+""")

    fun normalise(raw: String): String =
        raw
            .uppercase()
            .replace(NON_ALNUM, " ")
            .replace(STORE_NUMBER, " ")
            .replace(WHITESPACE, " ")
            .trim()

    fun bestMatch(
        input: String,
        vendors: List<Vendor>,
    ): VendorMatch? {
        val needle = normalise(input)
        if (needle.isEmpty()) return null

        vendors.firstOrNull { normalise(it.name) == needle }?.let {
            return VendorMatch(it, it.name)
        }

        for (vendor in vendors) {
            val hit = vendor.matchAliases.firstOrNull { normalise(it) == needle }
            if (hit != null) return VendorMatch(vendor, hit)
        }

        var best: VendorMatch? = null
        var bestLength = 0
        for (vendor in vendors) {
            for (candidate in listOf(vendor.name) + vendor.matchAliases) {
                val normalised = normalise(candidate)
                if (normalised.isEmpty()) continue
                val contains = needle.contains(normalised) || normalised.contains(needle)
                if (contains && normalised.length > bestLength) {
                    bestLength = normalised.length
                    best = VendorMatch(vendor, candidate)
                }
            }
        }
        return best
    }
}
