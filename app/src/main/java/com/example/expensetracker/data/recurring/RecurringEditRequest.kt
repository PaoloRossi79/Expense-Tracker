package com.paolorossi.expensetracker.data.recurring

import android.content.Intent
import java.time.LocalDate

/** "Edit & confirm" hand-off from a notification into the Add screen (Design/01 §4.7). */
data class RecurringEditRequest(
    val ruleId: String,
    val dueDate: LocalDate,
) {
    companion object {
        fun from(intent: Intent?): RecurringEditRequest? {
            if (intent?.action != RecurringNotifier.ACTION_EDIT) return null
            val ruleId = intent.getStringExtra(RecurringNotifier.EXTRA_RULE_ID) ?: return null
            val date =
                intent.getStringExtra(RecurringNotifier.EXTRA_DATE)
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: return null
            return RecurringEditRequest(ruleId, date)
        }
    }
}
