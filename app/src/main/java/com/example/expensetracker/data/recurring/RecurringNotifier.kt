package com.paolorossi.expensetracker.data.recurring

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.paolorossi.expensetracker.MainActivity
import com.paolorossi.expensetracker.R
import com.paolorossi.expensetracker.data.repository.PendingOccurrence
import com.paolorossi.expensetracker.domain.Money
import com.paolorossi.expensetracker.domain.RecurringSchedule
import java.time.LocalDate

/**
 * Builds and posts the recurring reminders, and defines the notification actions
 * Confirm / Edit & confirm / Skip (Design/01 §4.7).
 */
object RecurringNotifier {
    const val CHANNEL_ID = "recurring_reminders"

    const val ACTION_CONFIRM = "com.paolorossi.expensetracker.action.CONFIRM_RECURRING"
    const val ACTION_SKIP = "com.paolorossi.expensetracker.action.SKIP_RECURRING"
    const val ACTION_EDIT = "com.paolorossi.expensetracker.action.EDIT_RECURRING"

    const val EXTRA_RULE_ID = "ruleId"
    const val EXTRA_DATE = "date"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Recurring reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Reminders to confirm or skip recurring transactions"
            }
        manager.createNotificationChannel(channel)
    }

    fun notificationId(
        ruleId: String,
        date: LocalDate,
    ): Int = (ruleId + RecurringSchedule.occurrenceId(date)).hashCode()

    fun notify(
        context: Context,
        pending: List<PendingOccurrence>,
    ) {
        if (!canPost(context)) return
        pending.forEach { notifyOne(context, it) }
    }

    fun cancel(
        context: Context,
        ruleId: String,
        date: LocalDate,
    ) {
        NotificationManagerCompat.from(context).cancel(notificationId(ruleId, date))
    }

    private fun notifyOne(
        context: Context,
        item: PendingOccurrence,
    ) {
        val id = notificationId(item.rule.id, item.dueDate)
        val amount = Money.formatPence(item.rule.amountPence)

        val confirm = broadcast(context, id, ACTION_CONFIRM, item)
        val skip = broadcast(context, id, ACTION_SKIP, item)

        val editIntent =
            Intent(context, MainActivity::class.java).apply {
                action = ACTION_EDIT
                putExtra(EXTRA_RULE_ID, item.rule.id)
                putExtra(EXTRA_DATE, item.dueDate.toString())
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val edit =
            PendingIntent.getActivity(
                context,
                id,
                editIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("${item.rule.reason} $amount is due")
                .setContentText("Confirm, edit, or skip this month's occurrence")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .addAction(0, "Confirm", confirm)
                .addAction(0, "Edit & confirm", edit)
                .addAction(0, "Skip", skip)
                .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun broadcast(
        context: Context,
        id: Int,
        action: String,
        item: PendingOccurrence,
    ): PendingIntent {
        val intent =
            Intent(context, RecurringActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_RULE_ID, item.rule.id)
                putExtra(EXTRA_DATE, item.dueDate.toString())
            }
        return PendingIntent.getBroadcast(
            context,
            id * 10 + action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canPost(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return true
    }
}
