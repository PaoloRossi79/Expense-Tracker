package com.paolorossi.expensetracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.paolorossi.expensetracker.data.model.Category
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface CategoryRepository {
    fun observeCategories(householdId: String): Flow<List<Category>>

    suspend fun createCategory(
        householdId: String,
        category: Category,
    ): String

    suspend fun updateCategory(
        householdId: String,
        category: Category,
    )

    suspend fun deleteCategory(
        householdId: String,
        id: String,
    )
}

class FirestoreCategoryRepository(
    private val firestore: FirebaseFirestore,
) : CategoryRepository {
    private fun collection(householdId: String) = firestore.collection("households").document(householdId).collection("categories")

    override fun observeCategories(householdId: String): Flow<List<Category>> =
        callbackFlow {
            val registration =
                collection(householdId)
                    .orderBy("name", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.documents?.mapNotNull { it.toCategory() }.orEmpty())
                    }
            awaitClose { registration.remove() }
        }

    override suspend fun createCategory(
        householdId: String,
        category: Category,
    ): String = collection(householdId).add(category.toMap()).await().id

    override suspend fun updateCategory(
        householdId: String,
        category: Category,
    ) {
        collection(householdId).document(category.id).set(category.toMap()).await()
    }

    override suspend fun deleteCategory(
        householdId: String,
        id: String,
    ) {
        collection(householdId).document(id).delete().await()
    }
}
