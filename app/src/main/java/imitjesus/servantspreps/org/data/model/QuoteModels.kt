package imitjesus.servantspreps.org.data.model

data class ImitationQuote(
    val id: Int,
    val day_of_year: Int,
    val calendar_date: String,
    val book_number: Int,
    val book_title: String,
    val article_id: Int,
    val article_number: Int,
    val article_title: String,
    val sentence_index: Int,
    val topic: String,
    val title: String,
    val quote: String
)

data class ImitationQuoteResponse(val item: ImitationQuote)
