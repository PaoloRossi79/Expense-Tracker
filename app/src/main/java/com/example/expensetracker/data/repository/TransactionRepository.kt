package com.paolorossi.expensetracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.paolorossi.expensetracker.data.model.Transaction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface TransactionRepository {
    fun observeTransactions(householdId: String): Flow<List<Transaction>>

    suspend fun getTransaction(
        householdId: String,
        id: String,
    ): Transaction?

    suspend fun addTransaction(
        householdId: String,
        transaction: Transaction,
    ): String

    suspend fun updateTransaction(
        householdId: String,
        transaction: Transaction,
    )

    suspend fun deleteTransaction(
        householdId: String,
        id: String,
    )

    suspend fun countForCategory(
        householdId: String,
        categoryId: String,
    ): Int
}

class FirestoreTransactionRepository(
    private val firestore: FirebaseFirestore,
) : TransactionRepository {
    private fun collection(householdId: String) = firestore.collection("households").document(householdId).collection("transactions")

    override fun observeTransactions(householdId: String): Flow<List<Transaction>> =
        callbackFlow {
            val registration =
                collection(householdId)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.documents?.mapNotNull { it.toTransaction() }.orEmpty())
                    }
            awaitClose { registration.remove() }
        }

    override suspend fun getTransaction(
        householdId: String,
        id: String,
    ): Transaction? = collection(householdId).document(id).get().await().toTransaction()

    override suspend fun addTransaction(
        householdId: String,
        transaction: Transaction,
    ): String = collection(householdId).add(transaction.toMap()).await().id

    override suspend fun updateTransaction(
        householdId: String,
        transaction: Transaction,
    ) {
        collection(householdId).document(transaction.id).update(transaction.toUpdateMap()).await()
    }

    override suspend fun deleteTransaction(
        householdId: String,
        id: String,
    ) {
        collection(householdId).document(id).delete().await()
    }

    override suspend fun countForCategory(
        householdId: String,
        categoryId: String,
    ): Int = collection(householdId).whereEqualTo("categoryId", categoryId).get().await().size()
}
