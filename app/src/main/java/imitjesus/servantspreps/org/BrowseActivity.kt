package imitjesus.servantspreps.org

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import imitjesus.servantspreps.org.data.SettingsManager
import imitjesus.servantspreps.org.data.model.ImitationQuote
import imitjesus.servantspreps.org.data.model.Book
import imitjesus.servantspreps.org.data.repository.QuoteRepository
import imitjesus.servantspreps.org.databinding.ActivityBrowseBinding
import imitjesus.servantspreps.org.ui.BrowseAdapter
import imitjesus.servantspreps.org.ui.BrowseItem
import imitjesus.servantspreps.org.ui.BrowseType
import kotlinx.coroutines.launch

class BrowseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowseBinding
    private lateinit var settingsManager: SettingsManager
    private val repository = QuoteRepository()
    private val adapter = BrowseAdapter { handleItemClick(it) }
    
    private var allQuotes = listOf<ImitationQuote>()
    private var allBooks = listOf<Book>()
    
    private var selectedBook: Book? = null
    private var selectedChapter: Int? = null
    private var currentType = BrowseType.BOOK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.bottomNavigation.selectedItemId = R.id.nav_browse
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    true
                }
                R.id.nav_browse -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        styleSearchView()
        setupListeners()
        loadData()
        observeTheme()
    }

    private fun styleSearchView() {
        val textView = binding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        textView?.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        textView?.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary))

        val searchIcon = binding.searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon?.setColorFilter(ContextCompat.getColor(this, R.color.navy))

        val closeIcon = binding.searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeIcon?.setColorFilter(ContextCompat.getColor(this, R.color.navy))
    }

    private fun observeTheme() {
        lifecycleScope.launch {
            settingsManager.themeFlow.collect { theme ->
                applyTheme(theme)
            }
        }
    }

    private fun applyTheme(theme: String) {
        val (bg, primary, textCol, labelCol) = when (theme) {
            SettingsManager.THEME_DARK -> listOf(
                ContextCompat.getColor(this, R.color.navy_dark),
                ContextCompat.getColor(this, R.color.navy),
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.gold_light)
            )
            SettingsManager.THEME_FOREST -> listOf(
                ContextCompat.getColor(this, R.color.forest_bg),
                ContextCompat.getColor(this, R.color.forest_primary),
                ContextCompat.getColor(this, R.color.forest_text),
                ContextCompat.getColor(this, R.color.forest_primary)
            )
            SettingsManager.THEME_CRIMSON -> listOf(
                ContextCompat.getColor(this, R.color.crimson_bg),
                ContextCompat.getColor(this, R.color.crimson_primary),
                ContextCompat.getColor(this, R.color.crimson_text),
                ContextCompat.getColor(this, R.color.crimson_primary)
            )
            else -> listOf( // Cream
                ContextCompat.getColor(this, R.color.cream),
                ContextCompat.getColor(this, R.color.navy),
                ContextCompat.getColor(this, R.color.text_primary),
                ContextCompat.getColor(this, R.color.gold)
            )
        }
        binding.root.setBackgroundColor(bg)
        binding.toolbar.setBackgroundColor(primary)
        binding.tabLayout.setBackgroundColor(primary)
        binding.bottomNavigation.setBackgroundColor(primary)
        
        binding.tabLayout.setTabTextColors(
            ContextCompat.getColor(this, R.color.white),
            ContextCompat.getColor(this, R.color.gold_light)
        )
    }

    private fun setupListeners() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentType = when (tab?.position) {
                    0 -> BrowseType.BOOK
                    1 -> BrowseType.CHAPTER
                    2 -> BrowseType.TITLE
                    else -> BrowseType.BOOK
                }
                updateList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                updateList(query)
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                val isSearching = !newText.isNullOrBlank()
                binding.tabLayout.visibility = if (isSearching) View.GONE else View.VISIBLE
                updateList(newText)
                return true
            }
        })
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val booksResult = repository.getBooks()
            val quotesResult = repository.getAllQuotes()

            if (booksResult.isSuccess && quotesResult.isSuccess) {
                allBooks = booksResult.getOrNull() ?: emptyList()
                allQuotes = quotesResult.getOrNull() ?: emptyList()
                updateList()
            }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun updateList(query: String? = null) {
        val trimmed = query?.trim()?.takeIf { it.isNotEmpty() }

        val items = if (trimmed != null) {
            // Global search across all quotes regardless of current tab
            allQuotes.filter { quote ->
                quote.title?.contains(trimmed, ignoreCase = true) == true ||
                quote.quote?.contains(trimmed, ignoreCase = true) == true ||
                quote.source?.contains(trimmed, ignoreCase = true) == true
            }.map {
                BrowseItem(it.id, it.title ?: "Untitled", it.source ?: "", BrowseType.TITLE)
            }
        } else when (currentType) {
            BrowseType.BOOK -> {
                allBooks.map {
                    BrowseItem(it.id, "Book ${it.id}", it.title, BrowseType.BOOK)
                }
            }
            BrowseType.CHAPTER -> {
                val book = selectedBook
                if (book != null) {
                    val bookName = getBookName(book.id)
                    (1..book.chapterCount).map { chapterNum ->
                        val chapterTitle = allQuotes.find { quote ->
                            val source = quote.source ?: ""
                            source.contains(bookName, ignoreCase = true) &&
                            (source.contains("Chapter $chapterNum,", ignoreCase = true) ||
                             source.contains("Chapter $chapterNum", ignoreCase = true) ||
                             source.contains("Chapter ${String.format(java.util.Locale.US, "%02d", chapterNum)}", ignoreCase = true))
                        }?.title ?: ""
                        BrowseItem(chapterNum, "Chapter $chapterNum", chapterTitle, BrowseType.CHAPTER)
                    }
                } else {
                    listOf(BrowseItem(null, "Please select a book first", "", BrowseType.BOOK))
                }
            }
            BrowseType.TITLE -> {
                val book = selectedBook
                val chapter = selectedChapter

                allQuotes.filter { quote ->
                    val matchesSelection = if (book != null && chapter != null) {
                        val bookName = getBookName(book.id)
                        val source = quote.source ?: ""
                        source.contains(bookName, ignoreCase = true) &&
                        (source.contains("Chapter $chapter", ignoreCase = true) ||
                         source.contains("Chapter ${String.format(java.util.Locale.US, "%02d", chapter)}", ignoreCase = true))
                    } else if (book != null) {
                        val bookName = getBookName(book.id)
                        quote.source?.contains(bookName, ignoreCase = true) == true
                    } else true

                    matchesSelection
                }.map {
                    BrowseItem(it.id, it.title ?: "Untitled", it.source ?: "", BrowseType.TITLE)
                }
            }
        }
        adapter.submitList(items)
    }

    private fun getBookName(id: Int): String = when (id) {
        1 -> "Book One"
        2 -> "Book Two"
        3 -> "Book Three"
        4 -> "Book Four"
        else -> ""
    }

    private fun handleItemClick(item: BrowseItem) {
        when (item.type) {
            BrowseType.BOOK -> {
                selectedBook = allBooks.find { it.id == item.id }
                selectedChapter = null
                binding.tabLayout.getTabAt(1)?.select()
            }
            BrowseType.CHAPTER -> {
                selectedChapter = item.id
                binding.tabLayout.getTabAt(2)?.select()
            }
            BrowseType.TITLE -> {
                if (item.id != null) {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra("QUOTE_ID", item.id)
                    }
                    startActivity(intent)
                }
            }
        }
    }
}
