package com.paolorossi.expensetracker.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paolorossi.expensetracker.data.auth.AuthUser
import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.RecurringRule
import com.paolorossi.expensetracker.data.model.Transaction
import com.paolorossi.expensetracker.data.model.Vendor
import com.paolorossi.expensetracker.data.repository.CategoryRepository
import com.paolorossi.expensetracker.data.repository.PendingOccurrence
import com.paolorossi.expensetracker.data.repository.RecurringRepository
import com.paolorossi.expensetracker.data.repository.TransactionRepository
import com.paolorossi.expensetracker.data.repository.VendorRepository
import com.paolorossi.expensetracker.ui.addtransaction.AddTransactionViewModel
import com.paolorossi.expensetracker.ui.categories.CategoriesViewModel
import com.paolorossi.expensetracker.ui.home.HomeViewModel
import com.paolorossi.expensetracker.ui.transactions.TransactionsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Regression tests for TASK_01: tapping Home must always show Home, regardless of
 * which bottom-bar tab was visited before (Add Expense / Add Income / Transactions).
 */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val user = AuthUser(uid = "test-user", email = "test@example.com", displayName = "Tester")

    private fun launch() {
        composeRule.setContent {
            AppNavigation(
                user = user,
                editRequest = null,
                onEditConsumed = {},
                onSignOut = {},
                viewModelFactory = FakeViewModelFactory(),
            )
        }
        composeRule.waitForIdle()
    }

    private fun assertHomeShown() {
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test
    fun addExpenseThenAddIncomeThenHomeShowsHome() {
        launch()

        composeRule.onNodeWithText("Expense").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Income").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Home").performClick()
        composeRule.waitForIdle()

        assertHomeShown()
    }

    @Test
    fun addIncomeThenAddExpenseThenHomeShowsHome() {
        launch()

        composeRule.onNodeWithText("Income").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Expense").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Home").performClick()
        composeRule.waitForIdle()

        assertHomeShown()
    }

    @Test
    fun transactionsThenHomeShowsHome() {
        launch()

        composeRule.onNodeWithText("Transactions").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Home").performClick()
        composeRule.waitForIdle()

        assertHomeShown()
    }
}

private class FakeViewModelFactory(
    private val transactionRepository: TransactionRepository = FakeTransactionRepository(),
    private val categoryRepository: CategoryRepository = FakeCategoryRepository(),
    private val vendorRepository: VendorRepository = FakeVendorRepository(),
    private val recurringRepository: RecurringRepository = FakeRecurringRepository(),
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(transactionRepository, recurringRepository) as T

            modelClass.isAssignableFrom(AddTransactionViewModel::class.java) ->
                AddTransactionViewModel(
                    transactionRepository,
                    categoryRepository,
                    vendorRepository,
                    recurringRepository,
                ) as T

            modelClass.isAssignableFrom(TransactionsViewModel::class.java) ->
                TransactionsViewModel(
                    transactionRepository,
                    categoryRepository,
                ) { "test-user" } as T

            modelClass.isAssignableFrom(CategoriesViewModel::class.java) ->
                CategoriesViewModel(categoryRepository, transactionRepository) as T

            else -> error("Unexpected ViewModel: ${modelClass.name}")
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
    ): String = "transaction-id"

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

private class FakeCategoryRepository : CategoryRepository {
    override fun observeCategories(householdId: String): Flow<List<Category>> = flowOf(emptyList())

    override suspend fun createCategory(
        householdId: String,
        category: Category,
    ): String = "category-id"

    override suspend fun updateCategory(
        householdId: String,
        category: Category,
    ) = Unit

    override suspend fun deleteCategory(
        householdId: String,
        id: String,
    ) = Unit
}

private class FakeVendorRepository : VendorRepository {
    override fun observeVendors(householdId: String): Flow<List<Vendor>> = flowOf(emptyList())

    override suspend fun createVendor(
        householdId: String,
        vendor: Vendor,
    ): String = "vendor-id"

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
