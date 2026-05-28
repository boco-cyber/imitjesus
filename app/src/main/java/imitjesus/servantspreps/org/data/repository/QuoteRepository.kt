package imitjesus.servantspreps.org.data.repository

import imitjesus.servantspreps.org.data.api.RetrofitClient
import imitjesus.servantspreps.org.data.model.ImitationQuote

class QuoteRepository {
    suspend fun getTodayQuote(): Result<ImitationQuote> = runCatching {
        RetrofitClient.api.getToday().item
    }
}
