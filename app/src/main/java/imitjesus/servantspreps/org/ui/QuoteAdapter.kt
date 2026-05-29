package imitjesus.servantspreps.org.ui

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import imitjesus.servantspreps.org.R
import imitjesus.servantspreps.org.data.SettingsManager
import imitjesus.servantspreps.org.data.model.ImitationQuote
import imitjesus.servantspreps.org.databinding.ItemQuoteBinding
import java.util.Locale

class QuoteAdapter(
    private val onCopy: (ImitationQuote) -> Unit
) : ListAdapter<ImitationQuote, QuoteAdapter.QuoteViewHolder>(QuoteDiffCallback()) {

    private var fontSize: Float = 20f
    private var fontFamily: String = SettingsManager.FONT_SERIF
    private var theme: String = SettingsManager.THEME_CREAM

    fun updateSettings(fontSize: Float, fontFamily: String, theme: String) {
        this.fontSize = fontSize
        this.fontFamily = fontFamily
        this.theme = theme
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val binding = ItemQuoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QuoteViewHolder(private val binding: ItemQuoteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(q: ImitationQuote) {
            val context = binding.root.context
            
            // Parse Date
            val date = q.calendar_date ?: ""
            val (month, day) = if (date.contains("-")) {
                date.split("-").map { it.toIntOrNull() ?: 0 }
            } else listOf(0, 0)
            val months = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
            val monthName = if (month in 1..12) months[month - 1] else date
            binding.tvDate.text = "$monthName $day".uppercase(Locale.getDefault())

            // Parse Source for Chapter
            val fullSource = q.source ?: ""
            val chapterPart = when {
                fullSource.contains("Chapter", ignoreCase = true) -> {
                    if (fullSource.contains("—")) fullSource.substringAfter("—").trim()
                    else if (fullSource.contains("-")) fullSource.substringAfter("-").trim()
                    else fullSource
                }
                else -> fullSource
            }
            binding.tvChapter.text = chapterPart
            binding.tvTitle.text = q.title ?: ""

            // Quote
            binding.tvQuote.text = "“${q.quote ?: ""}”"
            binding.btnCopy.setOnClickListener { onCopy(q) }

            // Prayer
            binding.tvPrayer.text = q.prayer ?: ""

            // Author and Book
            binding.tvAuthor.text = context.getString(R.string.author_name)
            binding.tvBook.text = context.getString(R.string.book_name)

            // Apply Settings
            binding.tvQuote.textSize = fontSize
            binding.tvQuote.typeface = when (fontFamily) {
                SettingsManager.FONT_SANS -> Typeface.SANS_SERIF
                SettingsManager.FONT_MONO -> Typeface.MONOSPACE
                else -> Typeface.SERIF
            }

            applyTheme(context)
        }

        private fun applyTheme(context: Context) {
            val (bg, topBgTheme, _, textCol, labelCol) = when (theme) {
                SettingsManager.THEME_DARK -> listOf(
                    ContextCompat.getColor(context, R.color.navy_dark),
                    ContextCompat.getColor(context, R.color.navy),
                    ContextCompat.getColor(context, R.color.gold_light),
                    ContextCompat.getColor(context, R.color.white),
                    ContextCompat.getColor(context, R.color.gold_light)
                )
                SettingsManager.THEME_FOREST -> listOf(
                    ContextCompat.getColor(context, R.color.forest_bg),
                    ContextCompat.getColor(context, R.color.forest_primary),
                    ContextCompat.getColor(context, R.color.forest_primary),
                    ContextCompat.getColor(context, R.color.forest_text),
                    ContextCompat.getColor(context, R.color.forest_primary)
                )
                SettingsManager.THEME_CRIMSON -> listOf(
                    ContextCompat.getColor(context, R.color.crimson_bg),
                    ContextCompat.getColor(context, R.color.crimson_primary),
                    ContextCompat.getColor(context, R.color.crimson_primary),
                    ContextCompat.getColor(context, R.color.crimson_text),
                    ContextCompat.getColor(context, R.color.crimson_primary)
                )
                else -> listOf( // Cream
                    ContextCompat.getColor(context, R.color.cream),
                    ContextCompat.getColor(context, R.color.navy),
                    ContextCompat.getColor(context, R.color.navy),
                    ContextCompat.getColor(context, R.color.text_primary),
                    ContextCompat.getColor(context, R.color.gold)
                )
            }

            // Top Panel
            binding.cardTop.setCardBackgroundColor(topBgTheme)
            binding.tvDate.setTextColor(labelCol)
            binding.tvChapter.setTextColor(ContextCompat.getColor(context, R.color.white))
            binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.white))

            // Quote & Prayer Panels
            val isDark = theme == SettingsManager.THEME_DARK
            val cardBg = if (isDark) topBgTheme else ContextCompat.getColor(context, R.color.white)
            binding.cardQuote.setCardBackgroundColor(cardBg)
            binding.cardPrayer.setCardBackgroundColor(cardBg)
            
            binding.tvQuote.setTextColor(textCol)
            binding.tvPrayer.setTextColor(if (isDark) ContextCompat.getColor(context, R.color.white) else ContextCompat.getColor(context, R.color.text_secondary))
            binding.divider.setBackgroundColor(labelCol)

            // Bottom Panel
            val bottomBg = if (isDark) bg else ContextCompat.getColor(context, R.color.cream_dark)
            binding.cardBottom.setCardBackgroundColor(bottomBg)
            binding.tvAuthor.setTextColor(textCol)
            binding.tvBook.setTextColor(if (isDark) labelCol else ContextCompat.getColor(context, R.color.text_secondary))
            
            // Adjust Copy Button icon tint
            binding.btnCopy.setIconTintResource(if (isDark) R.color.gold_light else R.color.gold)
            binding.btnCopy.setTextColor(if (isDark) labelCol else ContextCompat.getColor(context, R.color.gold))
        }
    }
}

class QuoteDiffCallback : DiffUtil.ItemCallback<ImitationQuote>() {
    override fun areItemsTheSame(oldItem: ImitationQuote, newItem: ImitationQuote): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: ImitationQuote, newItem: ImitationQuote): Boolean {
        return oldItem == newItem
    }
}
