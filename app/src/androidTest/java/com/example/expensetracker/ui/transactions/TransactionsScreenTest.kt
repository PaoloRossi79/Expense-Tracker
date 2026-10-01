package com.paolorossi.expensetracker.ui.transactions

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.CategoryType
import com.paolorossi.expensetracker.data.model.Transaction
import com.paolorossi.expensetracker.data.model.TransactionType
import com.paolorossi.expensetracker.data.repository.CategoryRepository
import com.paolorossi.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * UI tests for the transactions list (Design/01 §7). The key negative case:
 * edit/delete controls must not appear on another member's transaction.
 */
@RunWith(AndroidJUnit4::class)
class TransactionsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val categories =
        listOf(
            Category(id = "c1", name = "Groceries", type = CategoryType.EXPENSE),
        )

    private val mine =
        Transaction(
            id = "mine",
            type = TransactionType.EXPENSE,
            amountPence = 100,
            date = LocalDate.of(2026, 3, 10),
            vendor = "My Shop",
            categoryId = "c1",
            createdByUid = "me",
            createdByName = "Me",
        )

    private val theirs =
        Transaction(
            id = "theirs",
            type = TransactionType.EXPENSE,
            amountPence = 200,
            date = LocalDate.of(2026, 3, 11),
            vendor = "Their Shop",
            categoryId = "c1",
            createdByUid = "other",
            createdByName = "Other",
        )

    private fun viewModel() =
        TransactionsViewModel(
            transactionRepository = FakeTransactionRepository(listOf(mine, theirs)),
            categoryRepository = FakeCategoryRepository(categories),
            currentUserUidProvider = { "me" },
        )

    @Test
    fun bothMembersTransactionsAreVisible() {
        composeRule.setContent { TransactionsScreen(onEdit = {}, viewModel = viewModel()) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("My Shop").assertExists()
        composeRule.onNodeWithText("Their Shop").assertExists()
    }

    @Test
    fun editDeleteControlsOnlyOnOwnTransaction() {
        composeRule.setContent { TransactionsScreen(onEdit = {}, viewModel = viewModel()) }
        composeRule.waitForIdle()
        // Exactly one row (the user's own) exposes the overflow actions menu.
        composeRule.onAllNodesWithContentDescription("Actions").assertCountEquals(1)
    }
}

private class FakeTransactionRepository(
    private val items: List<Transaction>,
) : TransactionRepository {
    override fun observeTransactions(householdId: String): Flow<List<Transaction>> = flowOf(items)

    override suspend fun getTransaction(
        householdId: String,
        id: String,
    ): Transaction? = items.firstOrNull { it.id == id }

    override suspend fun addTransaction(
        householdId: String,
        transaction: Transaction,
    ): String = transaction.id

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
    ): Int = items.count { it.categoryId == categoryId }
}

private class FakeCategoryRepository(
    private val items: List<Category>,
) : CategoryRepository {
    override fun observeCategories(householdId: String): Flow<List<Category>> = flowOf(items)

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
