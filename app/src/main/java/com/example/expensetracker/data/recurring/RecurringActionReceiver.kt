package com.paolorossi.expensetracker.data.recurring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.paolorossi.expensetracker.data.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Handles the notification Confirm / Skip actions (Design/01 §4.7). Runs a short
 * background coroutine and always finishes the async result.
 */
class RecurringActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val ruleId = intent.getStringExtra(RecurringNotifier.EXTRA_RULE_ID) ?: return
        val date =
            intent.getStringExtra(RecurringNotifier.EXTRA_DATE)
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: return
        if (!ServiceLocator.isConfigured) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val householdId = ServiceLocator.householdId
                val user = ServiceLocator.authRepository.currentUser
                val rule =
                    ServiceLocator.recurringRepository.observeRules(householdId)
                        .first()
                        .firstOrNull { it.id == ruleId }

                if (rule != null && user != null) {
                    when (intent.action) {
                        RecurringNotifier.ACTION_CONFIRM ->
                            ServiceLocator.recurringRepository
                                .confirmOccurrence(householdId, rule, date, user)

                        RecurringNotifier.ACTION_SKIP ->
                            ServiceLocator.recurringRepository
                                .skipOccurrence(householdId, rule, date, user.uid)
                    }
                }
                RecurringNotifier.cancel(context, ruleId, date)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
