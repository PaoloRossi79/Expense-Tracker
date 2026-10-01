package com.paolorossi.expensetracker.data.recurring

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Schedules the daily recurring reminder check. First run is at the next 09:00
 * local time (Design/01 §4.7 — configurable time is a §10 open question).
 */
object RecurringScheduler {
    const val DEFAULT_HOUR = 9

    fun schedule(
        context: Context,
        hour: Int = DEFAULT_HOUR,
    ) {
        val request =
            PeriodicWorkRequestBuilder<RecurringReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMillis(hour = hour), TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RecurringReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(RecurringReminderWorker.WORK_NAME)
    }

    internal fun initialDelayMillis(
        now: ZonedDateTime = ZonedDateTime.now(),
        hour: Int = DEFAULT_HOUR,
    ): Long {
        var next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }
}
