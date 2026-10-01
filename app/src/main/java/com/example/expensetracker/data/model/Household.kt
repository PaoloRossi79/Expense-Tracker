package com.paolorossi.expensetracker.data.model

import java.time.Instant

/**
 * The shared household. Membership is by lower-case email address
 * (`memberEmails`), which is the basis for Firestore security rules.
 */
data class Household(
    val id: String = "",
    val name: String,
    val createdAt: Instant? = null,
    val memberEmails: List<String> = emptyList(),
)
