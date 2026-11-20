package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.SettingsPreferences
import com.example.data.model.FinanceFormatters
import com.example.data.model.RecurringPayment
import com.example.data.remote.FinanceRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "spendwise_recurring_reminders"
private const val CHANNEL_NAME = "Recurring Payment Reminders"
private const val PERIODIC_WORK_NAME = "spendwise_daily_recurring_check"
private const val IMMEDIATE_WORK_NAME = "spendwise_immediate_recurring_check"
private const val TAG = "RecurringReminder"

object NotificationHelper {
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for upcoming recurring bills and scheduled income."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postDueReminderNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission denied when posting reminder", e)
        }
    }
}

class RecurringReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = SettingsPreferences(applicationContext).preferencesFlow.first()
        if (!prefs.notificationsEnabled) return Result.success()

        val uid = Firebase.auth.currentUser?.uid ?: return Result.success()
        val repository = FinanceRepository(applicationContext)

        return try {
            val activePayments = repository.fetchActiveRecurringOnce(uid)
            val now = System.currentTimeMillis()
            val dueSoon = activePayments.filter { payment ->
                val leadMillis = TimeUnit.DAYS.toMillis(payment.reminderDaysBefore.toLong())
                payment.nextDueMillis - now <= leadMillis
            }
            if (dueSoon.isNotEmpty()) {
                postSummaryNotification(dueSoon, prefs.currencyCode)
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to evaluate recurring payment reminders", e)
            Result.retry()
        }
    }

    private fun postSummaryNotification(duePayments: List<RecurringPayment>, currencyCode: String) {
        val first = duePayments.first()
        val amountStr = FinanceFormatters.formatCurrency(first.amount, currencyCode)
        val dueDateStr = FinanceFormatters.formatDateShort(first.nextDueMillis)
        val title = if (duePayments.size == 1) {
            "Upcoming: ${first.title}"
        } else {
            "${duePayments.size} Recurring Payments Due Soon"
        }
        val body = if (duePayments.size == 1) {
            "$amountStr (${first.category}) is due on $dueDateStr."
        } else {
            "${first.title} ($amountStr on $dueDateStr) and ${duePayments.size - 1} more need attention."
        }
        NotificationHelper.postDueReminderNotification(
            context = applicationContext,
            title = title,
            body = body,
            notificationId = 4102
        )
    }
}

object RecurringReminderScheduler {
    fun scheduleDailyCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecurringReminderWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelDailyCheck(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    fun triggerImmediateCheck(context: Context) {
        val request = OneTimeWorkRequestBuilder<RecurringReminderWorker>().build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
