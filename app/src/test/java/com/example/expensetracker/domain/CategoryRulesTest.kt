package com.paolorossi.expensetracker.domain

import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.CategoryType
import com.paolorossi.expensetracker.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryRulesTest {
    private val expenseCategory = Category(id = "e", name = "Groceries", type = CategoryType.EXPENSE)
    private val incomeCategory = Category(id = "i", name = "Salary", type = CategoryType.INCOME)

    @Test
    fun `expense category is valid only for expenses`() {
        assertTrue(CategoryRules.isValidFor(expenseCategory, TransactionType.EXPENSE))
        assertFalse(CategoryRules.isValidFor(expenseCategory, TransactionType.INCOME))
    }

    @Test
    fun `income category is valid only for income`() {
        assertTrue(CategoryRules.isValidFor(incomeCategory, TransactionType.INCOME))
        assertFalse(CategoryRules.isValidFor(incomeCategory, TransactionType.EXPENSE))
    }

    @Test
    fun `categoriesFor returns only matching type`() {
        val all = listOf(expenseCategory, incomeCategory)
        assertEquals(listOf(expenseCategory), CategoryRules.categoriesFor(TransactionType.EXPENSE, all))
        assertEquals(listOf(incomeCategory), CategoryRules.categoriesFor(TransactionType.INCOME, all))
    }

    @Test
    fun `unused category can be deleted`() {
        assertEquals(DeletionCheck.Allowed, CategoryRules.canDelete(0))
    }

    @Test
    fun `category in use is blocked with the usage count`() {
        val check = CategoryRules.canDelete(3)
        assertTrue(check is DeletionCheck.Blocked)
        assertEquals(3, (check as DeletionCheck.Blocked).usageCount)
    }
}
