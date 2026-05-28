package imitjesus.servantspreps.org.worker

import android.content.Context
import androidx.work.*
import imitjesus.servantspreps.org.data.repository.QuoteRepository
import imitjesus.servantspreps.org.notifications.NotificationHelper
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyQuoteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = QuoteRepository()
        val result = repository.getTodayQuote()

        result.onSuccess { entry ->
            NotificationHelper.showNotification(
                applicationContext,
                title = entry.title?.en ?: "Daily Bread",
                body = "\"${entry.verse.text.en}\"\n— ${entry.verse.reference.en}"
            )
        }

        scheduleNext(applicationContext)
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val WORK_NAME = "daily_quote_work"

        fun scheduleNext(context: Context) {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
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
