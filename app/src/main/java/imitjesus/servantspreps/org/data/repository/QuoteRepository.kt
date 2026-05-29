package imitjesus.servantspreps.org.data.repository

import imitjesus.servantspreps.org.data.api.RetrofitClient
import imitjesus.servantspreps.org.data.model.ImitationQuote
import imitjesus.servantspreps.org.data.model.Book

class QuoteRepository {
    suspend fun getTodayQuote(): Result<ImitationQuote> = runCatching {
        RetrofitClient.api.getToday().item
    }

    suspend fun getQuoteById(id: Int): Result<ImitationQuote> = runCatching {
        RetrofitClient.api.getQuote(id).item
    }

    suspend fun getAllQuotes(): Result<List<ImitationQuote>> = runCatching {
        val allQuotes = mutableListOf<ImitationQuote>()
        var offset = 0
        val limit = 100
        
        do {
            val response = RetrofitClient.api.getQuotes(limit, offset)
            allQuotes.addAll(response.items)
            offset += response.items.size
        } while (allQuotes.size < response.total && response.items.isNotEmpty())
        
        allQuotes
    }

    suspend fun getBooks(): Result<List<Book>> = runCatching {
        RetrofitClient.api.getBooks().items
    }
}
