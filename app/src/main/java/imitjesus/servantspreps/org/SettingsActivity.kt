package imitjesus.servantspreps.org

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import imitjesus.servantspreps.org.data.SettingsManager
import imitjesus.servantspreps.org.databinding.ActivitySettingsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsManager = SettingsManager(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadSettings()
        setupListeners()
        observeTheme()

        binding.bottomNavigation.selectedItemId = R.id.nav_settings
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_today -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    true
                }
                R.id.nav_browse -> {
                    startActivity(Intent(this, BrowseActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            val fontSize = settingsManager.fontSizeFlow.first()
            binding.sliderFontSize.value = fontSize

            val fontFamily = settingsManager.fontFamilyFlow.first()
            when (fontFamily) {
                SettingsManager.FONT_SERIF -> binding.toggleFont.check(R.id.btnFontSerif)
                SettingsManager.FONT_SANS -> binding.toggleFont.check(R.id.btnFontSans)
                SettingsManager.FONT_MONO -> binding.toggleFont.check(R.id.btnFontMono)
            }

            val theme = settingsManager.themeFlow.first()
            when (theme) {
                SettingsManager.THEME_CREAM -> binding.toggleTheme.check(R.id.btnThemeCream)
                SettingsManager.THEME_DARK -> binding.toggleTheme.check(R.id.btnThemeDark)
                SettingsManager.THEME_FOREST -> binding.toggleTheme.check(R.id.btnThemeForest)
                SettingsManager.THEME_CRIMSON -> binding.toggleTheme.check(R.id.btnThemeCrimson)
            }

            val time = settingsManager.notificationTimeFlow.first()
            binding.tvNotificationTime.text = time
        }
    }

    private fun setupListeners() {
        binding.sliderFontSize.addOnChangeListener { _, value, _ ->
            lifecycleScope.launch { settingsManager.setFontSize(value) }
        }

        binding.toggleFont.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val font = when (checkedId) {
                    R.id.btnFontSerif -> SettingsManager.FONT_SERIF
                    R.id.btnFontSans -> SettingsManager.FONT_SANS
                    R.id.btnFontMono -> SettingsManager.FONT_MONO
                    else -> SettingsManager.FONT_SERIF
                }
                lifecycleScope.launch { settingsManager.setFontFamily(font) }
            }
        }

        binding.toggleTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val theme = when (checkedId) {
                    R.id.btnThemeCream -> SettingsManager.THEME_CREAM
                    R.id.btnThemeDark -> SettingsManager.THEME_DARK
                    R.id.btnThemeForest -> SettingsManager.THEME_FOREST
                    R.id.btnThemeCrimson -> SettingsManager.THEME_CRIMSON
                    else -> SettingsManager.THEME_CREAM
                }
                lifecycleScope.launch { settingsManager.setTheme(theme) }
            }
        }

        binding.btnTimePicker.setOnClickListener {
            showTimePicker()
        }
    }

    private fun observeTheme() {
        lifecycleScope.launch {
            settingsManager.themeFlow.collect { theme ->
                applyTheme(theme)
            }
        }
    }

    private fun applyTheme(theme: String) {
        val (bg, cardBg, primary, textCol, labelCol) = when (theme) {
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
            SettingsManager.THEME_CRIMSON -> listOf(
                ContextCompat.getColor(this, R.color.crimson_bg),
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.crimson_primary),
                ContextCompat.getColor(this, R.color.crimson_text),
                ContextCompat.getColor(this, R.color.crimson_primary)
            )
            else -> listOf( // Cream
                ContextCompat.getColor(this, R.color.cream),
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.navy),
                ContextCompat.getColor(this, R.color.text_primary),
                ContextCompat.getColor(this, R.color.gold)
            )
        }

        binding.settingsRoot.setBackgroundColor(bg)
        binding.toolbar.setBackgroundColor(primary)
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        binding.bottomNavigation.setBackgroundColor(primary)

        // Cards
        binding.cardAppearance.setCardBackgroundColor(cardBg)
        binding.cardNotifications.setCardBackgroundColor(cardBg)

        // Labels
        binding.tvAppearanceLabel.setTextColor(labelCol)
        binding.tvNotificationsLabel.setTextColor(labelCol)

        // Item Labels
        binding.tvThemeLabel.setTextColor(textCol)
        binding.tvFontFamilyLabel.setTextColor(textCol)
        binding.tvFontSizeLabel.setTextColor(textCol)
        binding.tvReminderTimeLabel.setTextColor(textCol)
        
        // Active values
        binding.tvNotificationTime.setTextColor(primary)
        binding.sliderFontSize.thumbTintList = android.content.res.ColorStateList.valueOf(primary)
        binding.sliderFontSize.trackActiveTintList = android.content.res.ColorStateList.valueOf(primary)
        
        // Dividers
        binding.divider1.setBackgroundColor(ContextCompat.getColor(this, R.color.divider))
        binding.divider2.setBackgroundColor(ContextCompat.getColor(this, R.color.divider))

        // Buttons and Toggles - High Contrast Fix
        val buttonTextColors = android.content.res.ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(ContextCompat.getColor(this, R.color.white), primary)
        )
        val buttonBgColors = android.content.res.ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(primary, android.graphics.Color.TRANSPARENT)
        )
        
        listOf(binding.btnThemeCream, binding.btnThemeDark, binding.btnThemeForest, binding.btnThemeCrimson,
               binding.btnFontSerif, binding.btnFontSans, binding.btnFontMono).forEach { btn ->
            btn.setTextColor(buttonTextColors)
            btn.backgroundTintList = buttonBgColors
            btn.setStrokeColor(android.content.res.ColorStateList.valueOf(primary))
        }
    }

    private fun showTimePicker() {
        lifecycleScope.launch {
            val currentTime = settingsManager.notificationTimeFlow.first()
            val parts = currentTime.split(":")
            val hour = parts[0].toIntOrNull() ?: 8
            val minute = parts[1].toIntOrNull() ?: 0

            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("Select Notification Time")
                .build()

            picker.addOnPositiveButtonClickListener {
                val selectedTime = String.format(Locale.getDefault(), "%02d:%02d", picker.hour, picker.minute)
                binding.tvNotificationTime.text = selectedTime
                lifecycleScope.launch { settingsManager.setNotificationTime(selectedTime) }
            }

            picker.show(supportFragmentManager, "time_picker")
        }
    }
}
