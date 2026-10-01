package com.paolorossi.expensetracker.data.model

/**
 * A category is scoped to expenses or income — **never both** (Design/01 §4.6).
 * The type is fixed at creation and must not change once transactions reference it.
 */
enum class CategoryType { EXPENSE, INCOME }

data class Category(
    val id: String = "",
    val name: String,
    val type: CategoryType,
    val icon: String? = null,
    val color: String? = null,
)
