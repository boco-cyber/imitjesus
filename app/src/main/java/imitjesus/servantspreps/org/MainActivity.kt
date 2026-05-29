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
import imitjesus.servantspreps.org.data.model.ImitationQuote
import imitjesus.servantspreps.org.notifications.NotificationHelper
import imitjesus.servantspreps.org.ui.MainViewModel
import imitjesus.servantspreps.org.ui.QuoteAdapter
import imitjesus.servantspreps.org.ui.QuoteState
import imitjesus.servantspreps.org.worker.DailyQuoteWorker
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var settingsManager: SettingsManager
    private lateinit var quoteAdapter: QuoteAdapter

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
        
        setupViewPager()
        observeSettings()

        val specificId = intent.getIntExtra("QUOTE_ID", -1)
        viewModel.loadQuotes(if (specificId != -1) specificId else null)

        lifecycleScope.launch {
            DailyQuoteWorker.scheduleNext(this@MainActivity)
        }

        binding.btnRetry.setOnClickListener { viewModel.loadQuotes() }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> {
                    viewModel.loadQuotes()
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
                is QuoteState.Success -> showQuotes(state)
                is QuoteState.Error -> showError()
            }
        }
    }

    private fun setupViewPager() {
        quoteAdapter = QuoteAdapter { quote -> copyQuote(quote) }
        binding.viewPager.adapter = quoteAdapter
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
            combine(
                settingsManager.fontSizeFlow,
                settingsManager.fontFamilyFlow,
                settingsManager.themeFlow
            ) { size, font, theme ->
                Triple(size, font, theme)
            }.collect { (size, font, theme) ->
                quoteAdapter.updateSettings(size, font, theme)
                applyTheme(theme)
            }
        }
    }

    private fun applyTheme(theme: String) {
        val (bg, primary) = when (theme) {
            SettingsManager.THEME_DARK -> listOf(
                ContextCompat.getColor(this, R.color.navy_dark),
                ContextCompat.getColor(this, R.color.navy)
            )
            SettingsManager.THEME_FOREST -> listOf(
                ContextCompat.getColor(this, R.color.forest_bg),
                ContextCompat.getColor(this, R.color.forest_primary)
            )
            SettingsManager.THEME_CRIMSON -> listOf(
                ContextCompat.getColor(this, R.color.crimson_bg),
                ContextCompat.getColor(this, R.color.crimson_primary)
            )
            else -> listOf( // Cream
                ContextCompat.getColor(this, R.color.cream),
                ContextCompat.getColor(this, R.color.navy)
            )
        }

        binding.root.setBackgroundColor(bg)
        binding.appBarLayout.setBackgroundColor(primary)
        binding.bottomNavigation.setBackgroundColor(primary)
    }

    private fun showLoading() {
        binding.loadingView.visibility = View.VISIBLE
        binding.viewPager.visibility = View.GONE
        binding.errorView.visibility = View.GONE
    }

    private fun showError() {
        binding.errorView.visibility = View.VISIBLE
        binding.loadingView.visibility = View.GONE
        binding.viewPager.visibility = View.GONE
    }

    private fun showQuotes(state: QuoteState.Success) {
        binding.loadingView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.viewPager.visibility = View.VISIBLE

        quoteAdapter.submitList(state.quotes) {
            binding.viewPager.setCurrentItem(state.initialPosition, false)
        }
    }

    private fun shareQuote() {
        val currentPos = binding.viewPager.currentItem
        val q = quoteAdapter.currentList.getOrNull(currentPos) ?: return
        
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

    private fun copyQuote(q: ImitationQuote) {
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
            viewModel.loadQuotes(specificId)
        }
    }
}
