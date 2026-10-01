package com.paolorossi.expensetracker.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.paolorossi.expensetracker.data.model.Category
import com.paolorossi.expensetracker.data.model.CategoryType
import com.paolorossi.expensetracker.data.model.Household
import com.paolorossi.expensetracker.data.model.Occurrence
import com.paolorossi.expensetracker.data.model.OccurrenceStatus
import com.paolorossi.expensetracker.data.model.RecurringRule
import com.paolorossi.expensetracker.data.model.Transaction
import com.paolorossi.expensetracker.data.model.TransactionType
import com.paolorossi.expensetracker.data.model.Vendor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

/**
 * Firestore <-> model mapping. Models stay free of Firebase types so the
 * domain layer stays pure and unit-testable; all serialisation lives here.
 * Dates are UTC (Design/03 conventions).
 */

internal const val WIRE_EXPENSE = "expense"
internal const val WIRE_INCOME = "income"

internal fun LocalDate.toTimestamp(): Timestamp = Timestamp(Date.from(atStartOfDay(ZoneOffset.UTC).toInstant()))

internal fun Timestamp.toLocalDate(): LocalDate = toDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate()

internal fun Instant.toTimestamp(): Timestamp = Timestamp(epochSecond, nano)

internal fun TransactionType.toWire(): String = if (this == TransactionType.EXPENSE) WIRE_EXPENSE else WIRE_INCOME

internal fun CategoryType.toWire(): String = if (this == CategoryType.EXPENSE) WIRE_EXPENSE else WIRE_INCOME

internal fun wireToTransactionType(value: String?): TransactionType =
    if (value == WIRE_INCOME) TransactionType.INCOME else TransactionType.EXPENSE

internal fun wireToCategoryType(value: String?): CategoryType = if (value == WIRE_INCOME) CategoryType.INCOME else CategoryType.EXPENSE

internal fun Transaction.toMap(): Map<String, Any?> =
    mapOf(
        "type" to type.toWire(),
        "amount" to amountPence,
        "date" to date.toTimestamp(),
        "vendor" to vendor,
        "vendorId" to vendorId,
        "categoryId" to categoryId,
        "isRecurringInstance" to isRecurringInstance,
        "recurringRuleId" to recurringRuleId,
        "createdByUid" to createdByUid,
        "createdByName" to createdByName,
        "createdAt" to (createdAt?.toTimestamp() ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

/** Edit map deliberately omits `createdAt`/creator fields (creator-only edit). */
internal fun Transaction.toUpdateMap(): Map<String, Any?> =
    mapOf(
        "type" to type.toWire(),
        "amount" to amountPence,
        "date" to date.toTimestamp(),
        "vendor" to vendor,
        "vendorId" to vendorId,
        "categoryId" to categoryId,
        "updatedAt" to FieldValue.serverTimestamp(),
    )

internal fun DocumentSnapshot.toTransaction(): Transaction? {
    val categoryId = getString("categoryId") ?: return null
    val date = getTimestamp("date")?.toLocalDate() ?: return null
    return Transaction(
        id = id,
        type = wireToTransactionType(getString("type")),
        amountPence = getLong("amount") ?: 0L,
        date = date,
        vendor = getString("vendor").orEmpty(),
        vendorId = getString("vendorId"),
        categoryId = categoryId,
        isRecurringInstance = getBoolean("isRecurringInstance") ?: false,
        recurringRuleId = getString("recurringRuleId"),
        createdByUid = getString("createdByUid").orEmpty(),
        createdByName = getString("createdByName").orEmpty(),
        createdAt = getTimestamp("createdAt")?.toInstant(),
        updatedAt = getTimestamp("updatedAt")?.toInstant(),
    )
}

internal fun Category.toMap(): Map<String, Any?> =
    mapOf(
        "name" to name,
        "type" to type.toWire(),
        "icon" to icon,
        "color" to color,
    )

internal fun DocumentSnapshot.toCategory(): Category? {
    val name = getString("name") ?: return null
    return Category(
        id = id,
        name = name,
        type = wireToCategoryType(getString("type")),
        icon = getString("icon"),
        color = getString("color"),
    )
}

internal fun Vendor.toMap(): Map<String, Any?> =
    mapOf(
        "name" to name,
        "defaultCategoryId" to defaultCategoryId,
        "matchAliases" to matchAliases,
    )

internal fun DocumentSnapshot.toVendor(): Vendor? {
    val name = getString("name") ?: return null

    @Suppress("UNCHECKED_CAST")
    val aliases = get("matchAliases") as? List<String> ?: emptyList()
    return Vendor(
        id = id,
        name = name,
        defaultCategoryId = getString("defaultCategoryId"),
        matchAliases = aliases,
    )
}

internal fun RecurringRule.toMap(): Map<String, Any?> =
    mapOf(
        "type" to type.toWire(),
        "reason" to reason,
        "amount" to amountPence,
        "categoryId" to categoryId,
        "vendorId" to vendorId,
        "dayOfMonth" to dayOfMonth.toLong(),
        "startDate" to startDate.toTimestamp(),
        "endDate" to endDate?.toTimestamp(),
        "active" to active,
        "createdByUid" to createdByUid,
        "createdByName" to createdByName,
        "createdAt" to (createdAt?.toTimestamp() ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

internal fun DocumentSnapshot.toRecurringRule(): RecurringRule? {
    val categoryId = getString("categoryId") ?: return null
    val start = getTimestamp("startDate")?.toLocalDate() ?: return null
    return RecurringRule(
        id = id,
        type = wireToTransactionType(getString("type")),
        reason = getString("reason").orEmpty(),
        amountPence = getLong("amount") ?: 0L,
        categoryId = categoryId,
        vendorId = getString("vendorId"),
        dayOfMonth = (getLong("dayOfMonth") ?: 1L).toInt(),
        startDate = start,
        endDate = getTimestamp("endDate")?.toLocalDate(),
        active = getBoolean("active") ?: true,
        createdByUid = getString("createdByUid").orEmpty(),
        createdByName = getString("createdByName").orEmpty(),
        createdAt = getTimestamp("createdAt")?.toInstant(),
        updatedAt = getTimestamp("updatedAt")?.toInstant(),
    )
}

internal fun DocumentSnapshot.toHousehold(): Household? {
    val name = getString("name") ?: return null

    @Suppress("UNCHECKED_CAST")
    val emails = get("memberEmails") as? List<String> ?: emptyList()
    return Household(
        id = id,
        name = name,
        createdAt = getTimestamp("createdAt")?.toInstant(),
        memberEmails = emails,
    )
}

internal fun Occurrence.toMap(): Map<String, Any?> =
    mapOf(
        "status" to if (status == OccurrenceStatus.CONFIRMED) "confirmed" else "skipped",
        "resolvedByUid" to resolvedByUid,
        "resolvedAt" to (resolvedAt?.toTimestamp() ?: FieldValue.serverTimestamp()),
        "transactionId" to transactionId,
    )

internal fun DocumentSnapshot.toOccurrence(): Occurrence =
    Occurrence(
        id = id,
        status =
            if (getString("status") == "confirmed") {
                OccurrenceStatus.CONFIRMED
            } else {
                OccurrenceStatus.SKIPPED
            },
        resolvedByUid = getString("resolvedByUid").orEmpty(),
        resolvedAt = getTimestamp("resolvedAt")?.toInstant(),
        transactionId = getString("transactionId"),
    )
