package com.paolorossi.expensetracker.integration

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import com.paolorossi.expensetracker.data.model.Transaction
import com.paolorossi.expensetracker.data.model.TransactionType
import com.paolorossi.expensetracker.data.repository.FirestoreTransactionRepository
import com.paolorossi.expensetracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Repository read/write behaviour against the Firestore emulator (Design/01 §7). */
@RunWith(AndroidJUnit4::class)
class TransactionRepositoryIntegrationTest {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: TransactionRepository
    private val householdId = "it-transactions"

    @Before
    fun setUp() {
        firestore = FirestoreEmulator.firestore(ApplicationProvider.getApplicationContext())
        repository = FirestoreTransactionRepository(firestore)
    }

    @Test
    fun createReadUpdateDelete() =
        runBlocking {
            val transaction =
                Transaction(
                    type = TransactionType.EXPENSE,
                    amountPence = 1234,
                    date = LocalDate.of(2026, 3, 1),
                    vendor = "Tesco",
                    categoryId = "groceries",
                    createdByUid = "u1",
                    createdByName = "Paolo",
                )

            val id = repository.addTransaction(householdId, transaction)
            assertTrue(id.isNotBlank())

            val loaded = repository.getTransaction(householdId, id)
            assertNotNull(loaded)
            assertEquals(1234L, loaded!!.amountPence)
            assertEquals(LocalDate.of(2026, 3, 1), loaded.date)

            repository.updateTransaction(householdId, loaded.copy(amountPence = 2000))
            assertEquals(2000L, repository.getTransaction(householdId, id)!!.amountPence)

            repository.deleteTransaction(householdId, id)
            assertNull(repository.getTransaction(householdId, id))
        }

    @Test
    fun observesTransactionsOrderedByDateDescending() =
        runBlocking {
            repository.addTransaction(
                householdId,
                sample("older", LocalDate.of(2026, 1, 1)),
            )
            repository.addTransaction(
                householdId,
                sample("newer", LocalDate.of(2026, 3, 1)),
            )

            val observed = repository.observeTransactions(householdId).first()
            assertEquals(LocalDate.of(2026, 3, 1), observed.first().date)
        }

    @Test
    fun countsTransactionsForACategory() =
        runBlocking {
            repository.addTransaction(householdId, sample("a", LocalDate.of(2026, 3, 1), "bills"))
            repository.addTransaction(householdId, sample("b", LocalDate.of(2026, 3, 2), "bills"))
            repository.addTransaction(householdId, sample("c", LocalDate.of(2026, 3, 3), "food"))

            assertEquals(2, repository.countForCategory(householdId, "bills"))
            assertEquals(1, repository.countForCategory(householdId, "food"))
        }

    private fun sample(
        id: String,
        date: LocalDate,
        categoryId: String = "c1",
    ) = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amountPence = 100,
        date = date,
        vendor = "Vendor $id",
        categoryId = categoryId,
        createdByUid = "u1",
        createdByName = "Paolo",
    )
}
