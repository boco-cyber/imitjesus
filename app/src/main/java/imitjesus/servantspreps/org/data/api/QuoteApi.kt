package imitjesus.servantspreps.org.data.api

import imitjesus.servantspreps.org.data.model.DailyEntry
import retrofit2.http.GET
import retrofit2.http.Query

interface QuoteApi {
    @GET("today")
    suspend fun getToday(@Query("lang") lang: String = "en"): DailyEntry
}
