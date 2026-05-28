package imitjesus.servantspreps.org

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import imitjesus.servantspreps.org.databinding.ActivityMainBinding
import imitjesus.servantspreps.org.notifications.NotificationHelper
import imitjesus.servantspreps.org.ui.MainViewModel
import imitjesus.servantspreps.org.ui.QuoteState
import imitjesus.servantspreps.org.worker.DailyQuoteWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission granted or denied — notifications will work or silently skip */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        NotificationHelper.createChannel(this)
        requestNotificationPermissionIfNeeded()
        DailyQuoteWorker.scheduleNext(this)

        binding.btnRetry.setOnClickListener { viewModel.loadTodayQuote() }

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
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
        val entry = state.entry
        binding.loadingView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.scrollView.visibility = View.VISIBLE

        val displayDate = try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(entry.date)
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(parsed ?: Date())
        } catch (e: Exception) { entry.date }

        binding.tvDate.text = displayDate.uppercase(Locale.getDefault())
        binding.tvMonthTheme.text = entry.monthTheme.en
        binding.tvVerse.text = "“${entry.verse.text.en}”"
        binding.tvReference.text = entry.verse.reference.en
        binding.tvCommentary.text = entry.commentary.en
        binding.tvAuthor.text = "— ${entry.authorTag.en}"
        binding.tvPrayer.text = entry.prayer.en

        val title = entry.title?.en
        if (!title.isNullOrBlank()) {
            binding.tvTitle.text = title.uppercase(Locale.getDefault())
            binding.tvTitle.visibility = View.VISIBLE
        }
    }

    private fun shareQuote() {
        val state = viewModel.state.value as? QuoteState.Success ?: return
        val entry = state.entry
        val text = "“${entry.verse.text.en}”\n— ${entry.verse.reference.en}" +
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
}
