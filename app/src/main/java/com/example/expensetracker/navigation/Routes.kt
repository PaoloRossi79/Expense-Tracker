package com.paolorossi.expensetracker.navigation

import com.paolorossi.expensetracker.data.model.TransactionType

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val CATEGORIES = "categories"
    const val VENDORS = "vendors"
    const val RECURRING = "recurring"
    const val MEMBERS = "members"

    const val ADD = "add?type={type}&transactionId={transactionId}&ruleId={ruleId}&dueDate={dueDate}"
    const val ADD_EXPENSE = "add?type=expense"
    const val ADD_INCOME = "add?type=income"

    fun edit(
        transactionId: String,
        type: TransactionType,
    ): String = "add?type=${type.name.lowercase()}&transactionId=$transactionId"

    fun confirmRecurring(
        ruleId: String,
        dueDate: String,
    ): String = "add?type=expense&ruleId=$ruleId&dueDate=$dueDate"
}
