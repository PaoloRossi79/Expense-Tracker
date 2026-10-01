package com.paolorossi.expensetracker.data.recurring

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.paolorossi.expensetracker.data.ServiceLocator
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Daily background check that posts reminders for pending recurring occurrences
 * (Design/01 §4.7). No server code — each device does this locally. Timing is
 * approximate; the Home "Pending recurring" card is the reliable fallback.
 */
class RecurringReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!ServiceLocator.isConfigured) return Result.success()
        return runCatching {
            val today = LocalDate.now()
            val pending =
                ServiceLocator.recurringRepository
                    .observePendingOccurrences(ServiceLocator.householdId, today)
                    .first()
            RecurringNotifier.ensureChannel(applicationContext)
            RecurringNotifier.notify(applicationContext, pending)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val WORK_NAME = "recurring_reminder_daily"
    }
}
