package imitjesus.servantspreps.org

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import imitjesus.servantspreps.org.databinding.ActivityMainBinding
import imitjesus.servantspreps.org.data.SettingsManager
import imitjesus.servantspreps.org.notifications.NotificationHelper
import imitjesus.servantspreps.org.ui.MainViewModel
import imitjesus.servantspreps.org.ui.QuoteState
import imitjesus.servantspreps.org.worker.DailyQuoteWorker
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var settingsManager: SettingsManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted or denied silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager(this)
        setSupportActionBar(binding.toolbar)
        NotificationHelper.createChannel(this)
        requestNotificationPermissionIfNeeded()
        
        observeSettings()

        val specificId = intent.getIntExtra("QUOTE_ID", -1)
        if (specificId != -1) {
            viewModel.loadSpecificQuote(specificId)
        } else {
            viewModel.loadTodayQuote()
        }

        lifecycleScope.launch {
            DailyQuoteWorker.scheduleNext(this@MainActivity)
        }

        binding.btnRetry.setOnClickListener { viewModel.loadTodayQuote() }
        binding.btnCopy.setOnClickListener { copyQuote() }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> {
                    viewModel.loadTodayQuote()
                    true
                }
                R.id.nav_browse -> {
                    startActivity(Intent(this, BrowseActivity::class.java))
                    false
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false
                }
                else -> false
            }
        }

        viewModel.state.observe(this) { state ->
            when (state) {
                is QuoteState.Loading -> showLoading()
                is QuoteState.Success -> showQuote(state)
                is QuoteState.Error -> showError()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> { shareQuote(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun observeSettings() {
        lifecycleScope.launch {
            settingsManager.fontSizeFlow.collect { size ->
                binding.tvQuote.textSize = size
            }
        }
        lifecycleScope.launch {
            settingsManager.fontFamilyFlow.collect { font ->
                val typeface = when (font) {
                    SettingsManager.FONT_SANS -> android.graphics.Typeface.SANS_SERIF
                    SettingsManager.FONT_MONO -> android.graphics.Typeface.MONOSPACE
                    else -> android.graphics.Typeface.SERIF
                }
                binding.tvQuote.typeface = typeface
            }
        }
        lifecycleScope.launch {
            settingsManager.themeFlow.collect { theme ->
                applyTheme(theme)
            }
        }
    }

    private fun applyTheme(theme: String) {
        val (bg, _, primary, textCol, labelCol) = when (theme) {
            SettingsManager.THEME_DARK -> listOf(
                ContextCompat.getColor(this, R.color.navy_dark),
                ContextCompat.getColor(this, R.color.navy),
                ContextCompat.getColor(this, R.color.gold_light),
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.gold_light)
            )
            SettingsManager.THEME_FOREST -> listOf(
                ContextCompat.getColor(this, R.color.forest_bg),
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.forest_primary),
                ContextCompat.getColor(this, R.color.forest_text),
                ContextCompat.getColor(this, R.color.forest_primary)
            )
            SettingsManager.THEME_ROYAL -> listOf(
                ContextCompat.getColor(this, R.color.royal_bg),
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.royal_primary),
                ContextCompat.getColor(this, R.color.royal_text),
                ContextCompat.getColor(this, R.color.royal_primary)
            )
            else -> listOf( // Cream
                ContextCompat.getColor(this, R.color.cream),
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.navy),
                ContextCompat.getColor(this, R.color.text_primary),
                ContextCompat.getColor(this, R.color.gold)
            )
        }

        binding.root.setBackgroundColor(bg)
        binding.appBarLayout.setBackgroundColor(primary)
        binding.bottomNavigation.setBackgroundColor(primary)
        
        // Update Quote Card and Text Colors
        binding.tvTitle.setTextColor(primary)
        binding.tvQuote.setTextColor(textCol)
        binding.tvPrayer.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        
        // Labels with specific theme contrast
        binding.tvTopic.setTextColor(labelCol)
        binding.tvDate.setTextColor(ContextCompat.getColor(this, R.color.gold_light))
    }

    private fun showLoading() {
        binding.loadingView.visibility = View.VISIBLE
        binding.scrollView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
    }

    private fun showError() {
        binding.errorView.visibility = View.VISIBLE
        binding.loadingView.visibility = View.GONE
        binding.scrollView.visibility = View.GONE
    }

    private fun showQuote(state: QuoteState.Success) {
        val q = state.quote
        binding.loadingView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.scrollView.visibility = View.VISIBLE

        val date = q.calendar_date ?: ""
        val (month, day) = if (date.contains("-")) {
            date.split("-").map { it.toIntOrNull() ?: 0 }
        } else listOf(0, 0)
        
        val months = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val monthName = if (month in 1..12) months[month - 1] else date
        binding.tvDate.text = "$monthName $day".uppercase(Locale.getDefault())
        binding.tvSource.text = "Thomas à Kempis — The Imitation of Christ"
        binding.tvTopic.text = "" // New quotes don't have separate topic field
        binding.tvTitle.text = q.title ?: ""
        binding.tvQuote.text = "“${q.quote ?: ""}”"
        binding.tvBook.text = q.source ?: ""
        binding.tvArticle.text = "" // Source field contains book and chapter
        binding.tvPrayer.text = q.prayer ?: ""
    }

    private fun shareQuote() {
        val state = viewModel.state.value as? QuoteState.Success ?: return
        val q = state.quote
        val text = "“${q.quote ?: ""}”\n\n— ${q.source ?: ""}\n" +
            "Thomas à Kempis, The Imitation of Christ" +
            getString(R.string.share_suffix)
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                getString(R.string.share)
            )
        )
    }

    private fun copyQuote() {
        val state = viewModel.state.value as? QuoteState.Success ?: return
        val q = state.quote
        val text = "“${q.quote ?: ""}”\n\n— ${q.source ?: ""}\n" +
            "Thomas à Kempis, The Imitation of Christ"
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("quote", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val specificId = intent.getIntExtra("QUOTE_ID", -1)
        if (specificId != -1) {
            viewModel.loadSpecificQuote(specificId)
        }
    }
}
