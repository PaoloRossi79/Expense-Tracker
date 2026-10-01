package com.paolorossi.expensetracker.domain

import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.CategoryType
import com.paolorossi.expensetracker.data.model.TransactionType

/** Result of checking whether a category may be deleted. */
sealed interface DeletionCheck {
    data object Allowed : DeletionCheck

    data class Blocked(val usageCount: Int) : DeletionCheck
}

/**
 * Category-type integrity (Design/01 §4.6): an expense category is never valid
 * for income and vice versa, and a category in use must not be deleted.
 */
object CategoryRules {
    fun isValidFor(
        category: Category,
        type: TransactionType,
    ): Boolean =
        when (type) {
            TransactionType.EXPENSE -> category.type == CategoryType.EXPENSE
            TransactionType.INCOME -> category.type == CategoryType.INCOME
        }

    fun categoriesFor(
        type: TransactionType,
        categories: List<Category>,
    ): List<Category> = categories.filter { isValidFor(it, type) }

    fun canDelete(usageCount: Int): DeletionCheck = if (usageCount <= 0) DeletionCheck.Allowed else DeletionCheck.Blocked(usageCount)
}
