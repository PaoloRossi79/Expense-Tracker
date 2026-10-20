package com.paolorossi.expensetracker.ui.addtransaction

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paolorossi.expensetracker.data.auth.AuthUser
import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.CategoryType
import com.paolorossi.expensetracker.data.model.RecurringRule
import com.paolorossi.expensetracker.data.model.Transaction
import com.paolorossi.expensetracker.data.model.TransactionType
import com.paolorossi.expensetracker.data.model.Vendor
import com.paolorossi.expensetracker.data.repository.CategoryRepository
import com.paolorossi.expensetracker.data.repository.PendingOccurrence
import com.paolorossi.expensetracker.data.repository.RecurringRepository
import com.paolorossi.expensetracker.data.repository.TransactionRepository
import com.paolorossi.expensetracker.data.repository.VendorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * TASK_02B: picking a vendor suggestion fills the vendor and its last used category.
 */
@RunWith(AndroidJUnit4::class)
class AddTransactionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val groceries = Category(id = "groceries", name = "Groceries", type = CategoryType.EXPENSE)
    private val tesco = Vendor(key = "tesco", name = "Tesco", lastCategoryId = "groceries")

    private fun viewModel() =
        AddTransactionViewModel(
            transactionRepository = FakeTransactionRepository(),
            categoryRepository = FakeCategoryRepository(listOf(groceries)),
            vendorRepository = FakeVendorRepository(listOf(tesco)),
            recurringRepository = FakeRecurringRepository(),
            currentUserProvider = { AuthUser(uid = "user-1", email = "user@example.com", displayName = "Tester") },
        )

    @Test
    fun pickingTescoFillsItsLastUsedCategory() {
        composeRule.setContent {
            AddTransactionScreen(
                type = TransactionType.EXPENSE,
                transactionId = null,
                recurringRuleId = null,
                dueDate = null,
                onDone = {},
                onBack = {},
                viewModel = viewModel(),
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("vendor_field").performTextInput("Te")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Tesco").assertExists().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Tesco").assertExists()
        composeRule.onNodeWithText("Groceries").assertExists()
    }
}

private class FakeTransactionRepository : TransactionRepository {
    override fun observeTransactions(householdId: String): Flow<List<Transaction>> = flowOf(emptyList())

    override suspend fun getTransaction(
        householdId: String,
        id: String,
    ): Transaction? = null

    override suspend fun addTransaction(
        householdId: String,
        transaction: Transaction,
    ): String = "tx-1"

    override suspend fun updateTransaction(
        householdId: String,
        transaction: Transaction,
    ) = Unit

    override suspend fun deleteTransaction(
        householdId: String,
        id: String,
    ) = Unit

    override suspend fun countForCategory(
        householdId: String,
        categoryId: String,
    ): Int = 0
}

private class FakeCategoryRepository(
    private val categories: List<Category>,
) : CategoryRepository {
    override fun observeCategories(householdId: String): Flow<List<Category>> = flowOf(categories)

    override suspend fun createCategory(
        householdId: String,
        category: Category,
    ): String = category.id

    override suspend fun updateCategory(
        householdId: String,
        category: Category,
    ) = Unit

    override suspend fun deleteCategory(
        householdId: String,
        id: String,
    ) = Unit
}

private class FakeVendorRepository(
    private val vendors: List<Vendor>,
) : VendorRepository {
    override fun observeVendors(householdId: String): Flow<List<Vendor>> = flowOf(vendors)

    override suspend fun createVendor(
        householdId: String,
        vendor: Vendor,
    ): String = vendor.key

    override suspend fun updateVendor(
        householdId: String,
        vendor: Vendor,
    ) = Unit

    override suspend fun deleteVendor(
        householdId: String,
        id: String,
    ) = Unit

    override suspend fun recordUsage(
        rawName: String,
        categoryId: String?,
    ) = Unit
}

private class FakeRecurringRepository : RecurringRepository {
    override fun observeRules(householdId: String): Flow<List<RecurringRule>> = flowOf(emptyList())

    override fun observePendingOccurrences(
        householdId: String,
        today: LocalDate,
    ): Flow<List<PendingOccurrence>> = flowOf(emptyList())

    override suspend fun createRule(
        householdId: String,
        rule: RecurringRule,
    ): String = "rule-id"

    override suspend fun updateRule(
        householdId: String,
        rule: RecurringRule,
    ) = Unit

    override suspend fun deleteRule(
        householdId: String,
        id: String,
    ) = Unit

    override suspend fun setActive(
        householdId: String,
        id: String,
        active: Boolean,
    ) = Unit

    override suspend fun resolvedOccurrenceIds(
        householdId: String,
        ruleId: String,
    ): Set<String> = emptySet()

    override suspend fun confirmOccurrence(
        householdId: String,
        rule: RecurringRule,
        date: LocalDate,
        user: AuthUser,
        amountPence: Long,
        categoryId: String,
        vendor: String,
    ): Boolean = true

    override suspend fun skipOccurrence(
        householdId: String,
        rule: RecurringRule,
        date: LocalDate,
        userUid: String,
    ): Boolean = true
}
