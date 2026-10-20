package com.paolorossi.expensetracker.data.model

import java.time.Instant

/**
 * A known vendor. [key] is the Firestore document id (lower-case, trimmed,
 * single spaces), so the same vendor in any spelling maps to one document.
 */
data class Vendor(
    val key: String = "",
    val name: String = "",
    val defaultCategoryId: String? = null,
    val matchAliases: List<String> = emptyList(),
    val lastCategoryId: String? = null,
    val updatedAt: Instant? = null,
    val updatedBy: String? = null,
)
