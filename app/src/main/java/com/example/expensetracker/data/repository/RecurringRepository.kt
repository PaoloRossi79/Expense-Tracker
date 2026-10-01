package com.paolorossi.expensetracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.paolorossi.expensetracker.data.auth.AuthUser
import com.paolorossi.expensetracker.data.model.Occurrence
import com.paolorossi.expensetracker.data.model.OccurrenceStatus
import com.paolorossi.expensetracker.data.model.RecurringRule
import com.paolorossi.expensetracker.data.model.Transaction
import com.paolorossi.expensetracker.domain.RecurringSchedule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A recurring occurrence that is due but not yet confirmed or skipped. */
data class PendingOccurrence(val rule: RecurringRule, val dueDate: LocalDate)

interface RecurringRepository {
    fun observeRules(householdId: String): Flow<List<RecurringRule>>

    fun observePendingOccurrences(
        householdId: String,
        today: LocalDate,
    ): Flow<List<PendingOccurrence>>

    suspend fun createRule(
        householdId: String,
        rule: RecurringRule,
    ): String

    suspend fun updateRule(
        householdId: String,
        rule: RecurringRule,
    )

    suspend fun deleteRule(
        householdId: String,
        id: String,
    )

    suspend fun setActive(
        householdId: String,
        id: String,
        active: Boolean,
    )

    suspend fun resolvedOccurrenceIds(
        householdId: String,
        ruleId: String,
    ): Set<String>

    suspend fun confirmOccurrence(
        householdId: String,
        rule: RecurringRule,
        date: LocalDate,
        user: AuthUser,
        amountPence: Long = rule.amountPence,
        categoryId: String = rule.categoryId,
        vendor: String = rule.reason,
    ): Boolean

    suspend fun skipOccurrence(
        householdId: String,
        rule: RecurringRule,
        date: LocalDate,
        userUid: String,
    ): Boolean
}

class FirestoreRecurringRepository(
    private val firestore: FirebaseFirestore,
) : RecurringRepository {
    private fun rules(householdId: String) = firestore.collection("households").document(householdId).collection("recurringRules")

    private fun occurrences(
        householdId: String,
        ruleId: String,
    ) = rules(householdId).document(ruleId).collection("occurrences")

    override fun observeRules(householdId: String): Flow<List<RecurringRule>> =
        callbackFlow {
            val registration =
                rules(householdId)
                    .orderBy("dayOfMonth", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.documents?.mapNotNull { it.toRecurringRule() }.orEmpty())
                    }
            awaitClose { registration.remove() }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePendingOccurrences(
        householdId: String,
        today: LocalDate,
    ): Flow<List<PendingOccurrence>> =
        observeRules(householdId).flatMapLatest { rules ->
            flow { emit(computePending(householdId, rules, today)) }
        }

    override suspend fun createRule(
        householdId: String,
        rule: RecurringRule,
    ): String = rules(householdId).add(rule.toMap()).await().id

    override suspend fun updateRule(
        householdId: String,
        rule: RecurringRule,
    ) {
        rules(householdId).document(rule.id).set(rule.toMap()).await()
    }

    override suspend fun deleteRule(
        householdId: String,
        id: String,
    ) {
        rules(householdId).document(id).delete().await()
    }

    override suspend fun setActive(
        householdId: String,
        id: String,
        active: Boolean,
    ) {
        rules(householdId).document(id).update("active", active).await()
    }

    override suspend fun resolvedOccurrenceIds(
        householdId: String,
        ruleId: String,
    ): Set<String> = occurrences(householdId, ruleId).get().await().documents.map { it.id }.toSet()

    override suspend fun confirmOccurrence(
        householdId: String,
        rule: RecurringRule,
        date: LocalDate,
        user: AuthUser,
        amountPence: Long,
        categoryId: String,
        vendor: String,
    ): Boolean {
        val occurrenceId = RecurringSchedule.occurrenceId(date)
        val occurrenceRef = occurrences(householdId, rule.id).document(occurrenceId)
        val transactionId = RecurringSchedule.recurringTransactionId(rule.id, date)
        val transactionRef =
            firestore.collection("households").document(householdId)
                .collection("transactions").document(transactionId)

        if (occurrenceRef.get().await().exists()) return true

        val transaction =
            Transaction(
                id = transactionId,
                type = rule.type,
                amountPence = amountPence,
                date = date,
                vendor = vendor,
                vendorId = rule.vendorId,
                categoryId = categoryId,
                isRecurringInstance = true,
                recurringRuleId = rule.id,
                createdByUid = user.uid,
                createdByName = user.displayName,
            )
        val occurrence =
            Occurrence(
                id = occurrenceId,
                status = OccurrenceStatus.CONFIRMED,
                resolvedByUid = user.uid,
                transactionId = transactionId,
            )

        val committed =
            runCatching {
                val batch = firestore.batch()
                batch.set(transactionRef, transaction.toMap())
                batch.set(occurrenceRef, occurrence.toMap())
                batch.commit().await()
            }.isSuccess
        if (committed) return true
        // A concurrent confirm on another device may have won the create-only race.
        return runCatching { occurrenceRef.get().await().exists() }.getOrDefault(false)
    }

    override suspend fun skipOccurrence(
        householdId: String,
        rule: RecurringRule,
        date: LocalDate,
        userUid: String,
    ): Boolean {
        val occurrenceId = RecurringSchedule.occurrenceId(date)
        val occurrenceRef = occurrences(householdId, rule.id).document(occurrenceId)
        if (occurrenceRef.get().await().exists()) return true
        val occurrence =
            Occurrence(
                id = occurrenceId,
                status = OccurrenceStatus.SKIPPED,
                resolvedByUid = userUid,
                transactionId = null,
            )
        val committed = runCatching { occurrenceRef.set(occurrence.toMap()).await() }.isSuccess
        if (committed) return true
        return runCatching { occurrenceRef.get().await().exists() }.getOrDefault(false)
    }

    private suspend fun computePending(
        householdId: String,
        rules: List<RecurringRule>,
        today: LocalDate,
    ): List<PendingOccurrence> {
        val pending = mutableListOf<PendingOccurrence>()
        for (rule in rules) {
            if (!rule.active) continue
            val resolved =
                resolvedOccurrenceIds(householdId, rule.id)
                    .mapNotNull { runCatching { LocalDate.parse(it, OCCURRENCE_FORMAT) }.getOrNull() }
                    .toSet()
            RecurringSchedule.pendingDueDates(rule, today, resolved)
                .forEach { pending.add(PendingOccurrence(rule, it)) }
        }
        return pending.sortedBy { it.dueDate }
    }

    private companion object {
        private val OCCURRENCE_FORMAT: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
    }
}
