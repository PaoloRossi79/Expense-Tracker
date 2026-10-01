package com.paolorossi.expensetracker.domain

/** Basic email validation for the Add Member screen (Design/01 §4.9). */
object EmailValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValid(email: String): Boolean = EMAIL_REGEX.matches(email.trim())
}
