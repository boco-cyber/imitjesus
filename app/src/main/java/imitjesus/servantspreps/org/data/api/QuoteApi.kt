package imitjesus.servantspreps.org.data.api

import imitjesus.servantspreps.org.data.model.ImitationQuote
import imitjesus.servantspreps.org.data.model.ImitationQuoteResponse
import imitjesus.servantspreps.org.data.model.ImitationListResponse
import imitjesus.servantspreps.org.data.model.BookListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface QuoteApi {
    @GET("api/imitation/quotes/today")
    suspend fun getToday(): ImitationQuoteResponse

    @GET("api/imitation/quotes/{id}")
    suspend fun getQuote(@Path("id") id: Int): ImitationQuoteResponse

    @GET("api/imitation/quotes")
    suspend fun getQuotes(
        @Query("limit") limit: Int = 400,
        @Query("offset") offset: Int = 0
    ): ImitationListResponse

    @GET("api/imitation/books")
    suspend fun getBooks(): BookListResponse
}
