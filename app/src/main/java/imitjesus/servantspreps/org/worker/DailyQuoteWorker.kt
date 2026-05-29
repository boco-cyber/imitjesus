package imitjesus.servantspreps.org.worker

import android.content.Context
import androidx.work.*
import imitjesus.servantspreps.org.data.SettingsManager
import imitjesus.servantspreps.org.data.repository.QuoteRepository
import imitjesus.servantspreps.org.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyQuoteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = QuoteRepository()
        val result = repository.getTodayQuote()

        result.onSuccess { quote ->
            NotificationHelper.showNotification(
                applicationContext,
                title = quote.title ?: "Daily Quote",
                body = quote.quote ?: "Check today's spiritual reading."
            )
        }

        scheduleNext(applicationContext)
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val WORK_NAME = "daily_quote_work"

        suspend fun scheduleNext(context: Context) {
            val settingsManager = SettingsManager(context)
            val time = settingsManager.notificationTimeFlow.first()
            val parts = time.split(":")
            val hour = parts[0].toIntOrNull() ?: 8
            val minute = parts[1].toIntOrNull() ?: 0

            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            val delay = next.timeInMillis - now.timeInMillis

            val request = OneTimeWorkRequestBuilder<DailyQuoteWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
