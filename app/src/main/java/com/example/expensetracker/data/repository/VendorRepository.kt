package com.paolorossi.expensetracker.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.paolorossi.expensetracker.data.model.Vendor
import com.paolorossi.expensetracker.domain.VendorNameFormatter
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

    /** Call after a new expense is saved. Creates or updates the vendor. Ignores blank names. */
    suspend fun recordUsage(
        rawName: String,
        categoryId: String?,
    )
}

class FirestoreVendorRepository(
    private val firestore: FirebaseFirestore,
    private val householdId: String,
    private val currentUserId: () -> String?,
) : VendorRepository {
    private fun collection(householdId: String) = firestore.collection("households").document(householdId).collection("vendors")

    override fun observeVendors(householdId: String): Flow<List<Vendor>> =
        callbackFlow {
            val registration =
                collection(householdId)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
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
        collection(householdId).document(vendor.key).set(vendor.toMap()).await()
    }

    override suspend fun deleteVendor(
        householdId: String,
        id: String,
    ) {
        collection(householdId).document(id).delete().await()
    }

    override suspend fun recordUsage(
        rawName: String,
        categoryId: String?,
    ) {
        val name = VendorNameFormatter.toDisplayName(rawName)
        if (name.isEmpty()) return
        val key = VendorNameFormatter.toKey(rawName)
        val payload =
            mapOf(
                "name" to name,
                "lastCategoryId" to categoryId,
                "updatedAt" to FieldValue.serverTimestamp(),
                "updatedBy" to currentUserId(),
            )
        collection(householdId).document(key).set(payload, SetOptions.merge()).await()
    }
}
