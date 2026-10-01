package com.paolorossi.expensetracker.data.model

/**
 * A known vendor with an optional default category and extra match aliases used
 * by vendor auto-allocation (Design/01 §4.8).
 */
data class Vendor(
    val id: String = "",
    val name: String,
    val defaultCategoryId: String? = null,
    val matchAliases: List<String> = emptyList(),
)
