package imitjesus.servantspreps.org.data.api

import imitjesus.servantspreps.org.data.model.ImitationQuoteResponse
import retrofit2.http.GET

interface QuoteApi {
    @GET("api/imitation/quotes/today")
    suspend fun getToday(): ImitationQuoteResponse
}
