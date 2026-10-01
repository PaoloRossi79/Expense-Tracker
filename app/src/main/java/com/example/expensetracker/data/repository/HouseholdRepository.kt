package com.paolorossi.expensetracker.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.paolorossi.expensetracker.data.model.Household
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface HouseholdRepository {
    fun observeHousehold(householdId: String): Flow<Household?>

    suspend fun getHousehold(householdId: String): Household?

    suspend fun addMember(
        householdId: String,
        email: String,
    ): Boolean
}

class FirestoreHouseholdRepository(
    private val firestore: FirebaseFirestore,
) : HouseholdRepository {
    private fun document(householdId: String) = firestore.collection("households").document(householdId)

    override fun observeHousehold(householdId: String): Flow<Household?> =
        callbackFlow {
            val registration =
                document(householdId).addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.toHousehold())
                }
            awaitClose { registration.remove() }
        }

    override suspend fun getHousehold(householdId: String): Household? = document(householdId).get().await().toHousehold()

    override suspend fun addMember(
        householdId: String,
        email: String,
    ): Boolean {
        val normalised = email.trim().lowercase()
        val household = getHousehold(householdId) ?: return false
        if (household.memberEmails.any { it.equals(normalised, ignoreCase = true) }) return false
        // Append-only: the security rules reject any update that removes an entry.
        document(householdId).update("memberEmails", FieldValue.arrayUnion(normalised)).await()
        return true
    }
}
