package com.paolorossi.expensetracker.navigation

import com.paolorossi.expensetracker.data.model.TransactionType

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val CATEGORIES = "categories"
    const val VENDORS = "vendors"
    const val RECURRING = "recurring"
    const val MEMBERS = "members"

    const val ADD_EXPENSE = "add/expense"
    const val ADD_INCOME = "add/income"

    /** NavHost destination patterns; all three query arguments are optional. */
    const val ADD_EXPENSE_ROUTE =
        "add/expense?transactionId={transactionId}&ruleId={ruleId}&dueDate={dueDate}"
    const val ADD_INCOME_ROUTE =
        "add/income?transactionId={transactionId}&ruleId={ruleId}&dueDate={dueDate}"

    fun edit(
        transactionId: String,
        type: TransactionType,
    ): String = "add/${type.name.lowercase()}?transactionId=$transactionId"

    fun confirmRecurring(
        ruleId: String,
        dueDate: String,
    ): String = "add/expense?ruleId=$ruleId&dueDate=$dueDate"
}
