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
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied silently */ }

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
        val q = state.quote
        binding.loadingView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.scrollView.visibility = View.VISIBLE

        val (month, day) = q.calendar_date.split("-").map { it.toIntOrNull() ?: 0 }
        val months = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val monthName = if (month in 1..12) months[month - 1] else q.calendar_date
        binding.tvDate.text = "$monthName $day".uppercase(Locale.getDefault())
        binding.tvSource.text = "Thomas à Kempis — The Imitation of Christ"
        binding.tvTopic.text = q.topic.uppercase(Locale.getDefault())
        binding.tvTitle.text = q.title
        binding.tvQuote.text = "“${q.quote}”"
        binding.tvBook.text = "Book ${q.book_number} — ${q.book_title}"
        binding.tvArticle.text = "Chapter ${q.article_number}: ${q.article_title}"
    }

    private fun shareQuote() {
        val state = viewModel.state.value as? QuoteState.Success ?: return
        val q = state.quote
        val text = "“${q.quote}”\n\n— ${q.book_title}, Ch. ${q.article_number}\n" +
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
}
