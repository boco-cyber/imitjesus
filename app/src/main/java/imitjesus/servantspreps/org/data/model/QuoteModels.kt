package imitjesus.servantspreps.org.data.model

import com.google.gson.annotations.SerializedName

data class ImitationQuote(
    @SerializedName("id") val id: Int,
    @SerializedName("day") val day: Int,
    @SerializedName("calendar_date") val calendar_date: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("source") val source: String?,
    @SerializedName("body") val quote: String?,
    @SerializedName("prayer") val prayer: String?
)

data class ImitationQuoteResponse(val item: ImitationQuote)

data class ImitationListResponse(
    val items: List<ImitationQuote>,
    val total: Int
)

data class Book(
    val id: Int,
    val title: String,
    val chapterCount: Int
)

data class BookListResponse(
    val items: List<Book>
)
