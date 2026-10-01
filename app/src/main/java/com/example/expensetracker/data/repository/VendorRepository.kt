package com.paolorossi.expensetracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.paolorossi.expensetracker.data.model.Vendor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface VendorRepository {
    fun observeVendors(householdId: String): Flow<List<Vendor>>

    suspend fun createVendor(
        householdId: String,
        vendor: Vendor,
    ): String

    suspend fun updateVendor(
        householdId: String,
        vendor: Vendor,
    )

    suspend fun deleteVendor(
        householdId: String,
        id: String,
    )
}

class FirestoreVendorRepository(
    private val firestore: FirebaseFirestore,
) : VendorRepository {
    private fun collection(householdId: String) = firestore.collection("households").document(householdId).collection("vendors")

    override fun observeVendors(householdId: String): Flow<List<Vendor>> =
        callbackFlow {
            val registration =
                collection(householdId)
                    .orderBy("name", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.documents?.mapNotNull { it.toVendor() }.orEmpty())
                    }
            awaitClose { registration.remove() }
        }

    override suspend fun createVendor(
        householdId: String,
        vendor: Vendor,
    ): String = collection(householdId).add(vendor.toMap()).await().id

    override suspend fun updateVendor(
        householdId: String,
        vendor: Vendor,
    ) {
        collection(householdId).document(vendor.id).set(vendor.toMap()).await()
    }

    override suspend fun deleteVendor(
        householdId: String,
        id: String,
    ) {
        collection(householdId).document(id).delete().await()
    }
}
