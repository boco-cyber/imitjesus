package imitjesus.servantspreps.org.data.repository

import imitjesus.servantspreps.org.data.api.RetrofitClient
import imitjesus.servantspreps.org.data.model.DailyEntry

class QuoteRepository {
    suspend fun getTodayQuote(): Result<DailyEntry> = runCatching {
        RetrofitClient.api.getToday()
    }
}
