package com.paolorossi.expensetracker.ui.addtransaction

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val user = AuthUser(uid = "user-1", email = "user@example.com", displayName = "Tester")
    private val category = Category(id = "cat-groceries", name = "Groceries", type = CategoryType.EXPENSE)

    private lateinit var transactions: FakeTransactionRepository
    private lateinit var vendors: FakeVendorRepository
    private lateinit var viewModel: AddTransactionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        transactions = FakeTransactionRepository()
        vendors = FakeVendorRepository()
        viewModel =
            AddTransactionViewModel(
                transactionRepository = transactions,
                categoryRepository = FakeCategoryRepository(),
                vendorRepository = vendors,
                recurringRepository = FakeRecurringRepository(),
                currentUserProvider = { user },
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saving an expense records vendor usage once and stores the formatted name`() =
        runTest(dispatcher) {
            viewModel.onAmountChange("5.00")
            viewModel.onVendorChange("  tesco ")
            viewModel.onCategorySelect(category)

            viewModel.save()
            advanceUntilIdle()

            assertEquals("Tesco", transactions.added.single().vendor)
            assertEquals(listOf("  tesco " to "cat-groceries"), vendors.usage)
            assertTrue(viewModel.state.value.finished)
        }

    @Test
    fun `saving with a blank vendor does not crash`() =
        runTest(dispatcher) {
            viewModel.onAmountChange("5.00")
            viewModel.onVendorChange("   ")
            viewModel.onCategorySelect(category)

            viewModel.save()
            advanceUntilIdle()

            assertEquals("", transactions.added.single().vendor)
            assertTrue(viewModel.state.value.finished)
            assertNull(viewModel.state.value.error)
        }

    @Test
    fun `vendor recording failure does not fail the expense save`() =
        runTest(dispatcher) {
            vendors.failOnRecordUsage = true
            viewModel.onAmountChange("5.00")
            viewModel.onVendorChange("tesco")
            viewModel.onCategorySelect(category)

            viewModel.save()
            advanceUntilIdle()

            assertEquals(1, transactions.added.size)
            assertTrue(viewModel.state.value.finished)
            assertNull(viewModel.state.value.error)
        }

    @Test
    fun `selecting a vendor fills its name and last used category`() =
        runTest(dispatcher) {
            val groceries = Category(id = "groceries", name = "Groceries", type = CategoryType.EXPENSE)
            val transport = Category(id = "transport", name = "Transport", type = CategoryType.EXPENSE)
            val tesco = Vendor(key = "tesco", name = "Tesco", lastCategoryId = "groceries")
            val viewModel = viewModelWith(categories = listOf(groceries, transport), vendors = listOf(tesco))
            viewModel.load(TransactionType.EXPENSE, null, null, null)
            advanceUntilIdle()

            viewModel.onVendorSelected(tesco)

            assertEquals("Tesco", viewModel.state.value.vendor)
            assertEquals("groceries", viewModel.state.value.categoryId)
        }

    @Test
    fun `selecting a vendor keeps the category when its last category is gone`() =
        runTest(dispatcher) {
            val transport = Category(id = "transport", name = "Transport", type = CategoryType.EXPENSE)
            val tesco = Vendor(key = "tesco", name = "Tesco", lastCategoryId = "deleted-cat")
            val viewModel = viewModelWith(categories = listOf(transport), vendors = listOf(tesco))
            viewModel.load(TransactionType.EXPENSE, null, null, null)
            advanceUntilIdle()
            viewModel.onCategorySelect(transport)

            viewModel.onVendorSelected(tesco)

            assertEquals("Tesco", viewModel.state.value.vendor)
            assertEquals("transport", viewModel.state.value.categoryId)
        }

    @Test
    fun `typing a vendor in an expense does not auto-fill the category`() =
        runTest(dispatcher) {
            val groceries = Category(id = "groceries", name = "Groceries", type = CategoryType.EXPENSE)
            val transport = Category(id = "transport", name = "Transport", type = CategoryType.EXPENSE)
            val tesco = Vendor(key = "tesco", name = "Tesco", lastCategoryId = "groceries")
            val viewModel = viewModelWith(categories = listOf(groceries, transport), vendors = listOf(tesco))
            viewModel.load(TransactionType.EXPENSE, null, null, null)
            advanceUntilIdle()
            viewModel.onCategorySelect(transport)

            viewModel.onVendorChange("tes")

            assertEquals("tes", viewModel.state.value.vendor)
            assertEquals("transport", viewModel.state.value.categoryId)
        }

    private fun viewModelWith(
        categories: List<Category>,
        vendors: List<Vendor>,
    ) = AddTransactionViewModel(
        transactionRepository = transactions,
        categoryRepository = FakeCategoryRepository(categories),
        vendorRepository = FakeVendorRepository(vendors),
        recurringRepository = FakeRecurringRepository(),
        currentUserProvider = { user },
    )
}

private class FakeTransactionRepository : TransactionRepository {
    val added = mutableListOf<Transaction>()

    override fun observeTransactions(householdId: String): Flow<List<Transaction>> = flowOf(emptyList())

    override suspend fun getTransaction(
        householdId: String,
        id: String,
    ): Transaction? = null

    override suspend fun addTransaction(
        householdId: String,
        transaction: Transaction,
    ): String {
        added += transaction
        return "tx-${added.size}"
    }

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
    private val categories: List<Category> = emptyList(),
) : CategoryRepository {
    override fun observeCategories(householdId: String): Flow<List<Category>> = flowOf(categories)

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

private class FakeVendorRepository(
    private val vendors: List<Vendor> = emptyList(),
) : VendorRepository {
    val usage = mutableListOf<Pair<String, String?>>()
    var failOnRecordUsage = false

    override fun observeVendors(householdId: String): Flow<List<Vendor>> = flowOf(vendors)

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
    ) {
        usage += rawName to categoryId
        if (failOnRecordUsage) error("record usage failed")
    }
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
