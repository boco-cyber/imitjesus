package imitjesus.servantspreps.org

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import imitjesus.servantspreps.org.worker.DailyQuoteWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyQuoteWorker.scheduleNext(context)
        }
    }
}
