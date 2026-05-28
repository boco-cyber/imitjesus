package imitjesus.servantspreps.org.data.model

data class BilingualText(val en: String, val ar: String)

data class VerseText(val en: String, val ar: String)

data class VerseReference(val en: String, val ar: String)

data class Verse(val text: VerseText, val reference: VerseReference)

data class DailyEntry(
    val language: String,
    val dateKey: String,
    val date: String,
    val month: Int,
    val day: Int,
    val verse: Verse,
    val commentary: BilingualText,
    val prayer: BilingualText,
    val monthTheme: BilingualText,
    val authorTag: BilingualText,
    val title: BilingualText?
)
