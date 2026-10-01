package com.paolorossi.expensetracker.integration

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.CategoryType
import com.paolorossi.expensetracker.data.repository.CategoryRepository
import com.paolorossi.expensetracker.data.repository.FirestoreCategoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Category CRUD against the Firestore emulator (Design/01 §7). */
@RunWith(AndroidJUnit4::class)
class CategoryRepositoryIntegrationTest {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: CategoryRepository
    private val householdId = "it-categories"

    @Before
    fun setUp() {
        firestore = FirestoreEmulator.firestore(ApplicationProvider.getApplicationContext())
        repository = FirestoreCategoryRepository(firestore)
    }

    @Test
    fun createUpdateDeleteCategory() =
        runBlocking {
            val id =
                repository.createCategory(
                    householdId,
                    Category(name = "Groceries", type = CategoryType.EXPENSE),
                )
            assertTrue(id.isNotBlank())

            val created = repository.observeCategories(householdId).first().first { it.id == id }
            assertEquals(CategoryType.EXPENSE, created.type)

            repository.updateCategory(householdId, created.copy(name = "Food"))
            val renamed = repository.observeCategories(householdId).first().first { it.id == id }
            assertEquals("Food", renamed.name)

            repository.deleteCategory(householdId, id)
            assertTrue(repository.observeCategories(householdId).first().none { it.id == id })
        }

    @Test
    fun keepsExpenseAndIncomeCategoriesSeparate() =
        runBlocking {
            repository.createCategory(householdId, Category(name = "Salary", type = CategoryType.INCOME))
            repository.createCategory(householdId, Category(name = "Bills", type = CategoryType.EXPENSE))

            val categories = repository.observeCategories(householdId).first()
            val expenseNames = categories.filter { it.type == CategoryType.EXPENSE }.map { it.name }
            val incomeNames = categories.filter { it.type == CategoryType.INCOME }.map { it.name }
            assertTrue(expenseNames.contains("Bills"))
            assertTrue(incomeNames.contains("Salary"))
        }
}
