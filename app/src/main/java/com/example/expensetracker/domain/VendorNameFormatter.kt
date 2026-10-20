package com.paolorossi.expensetracker.domain

import java.util.Locale

object VendorNameFormatter {
    /** "  tesco   EXTRA " -> "Tesco Extra" */
    fun toDisplayName(raw: String): String =
        raw.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                word.lowercase(Locale.ROOT)
                    .replaceFirstChar { it.titlecase(Locale.ROOT) }
            }

    /** Firestore document id: same vendor in any spelling gives the same key. */
    fun toKey(raw: String): String = toDisplayName(raw).lowercase(Locale.ROOT).replace("/", "_")
}
