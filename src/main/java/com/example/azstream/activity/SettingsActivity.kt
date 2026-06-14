package com.example.azstream.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.azstream.R
import com.example.azstream.managers.SettingsData
import com.example.azstream.managers.SettingsManager
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider

class SettingsActivity : BaseActivity() {

    private lateinit var sliderInterval: Slider
    private lateinit var sliderVideoDuration: Slider
    private lateinit var sliderScreenshotSimilarity: Slider
    private lateinit var textIntervalValue: TextView
    private lateinit var textVideoDurationValue: TextView
    private lateinit var textScreenshotSimilarityValue: TextView
    private lateinit var buttonSaveSettings: Button
    private lateinit var editTextToken: EditText
    private lateinit var chipGroupQuality: ChipGroup

    private lateinit var settingsManager: SettingsManager
    private lateinit var currentSettings: SettingsData


    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        setupDrawer()
        setupWindowInsets()

        initializationObject()

        settingsManager = SettingsManager(this, this.lifecycleScope)

        // Загружаем сохранённые настройки
        currentSettings = settingsManager.loadSettings()

        // Применяем к UI
        applySettingsToUi()

        // Настройка слушателей слайдеров
        setupSliders()

        // Кнопка сохранения (одна, а не две!)
        buttonSaveSettings.setOnClickListener {
            saveAllSettings()
        }

        chipGroupQuality.setOnCheckedChangeListener { _, checkedId ->
            val quality = when (checkedId) {
                R.id.chipLow -> 0
                R.id.chipMedium -> 1
                R.id.chipHigh -> 2
                else -> 2
            }
            currentSettings.videoQuality = quality
        }
    }

    private fun applySettingsToUi() {
        editTextToken.setText(currentSettings.token)
        sliderInterval.value = currentSettings.refreshInterval.toFloat()
        sliderVideoDuration.value = currentSettings.videoDuration.toFloat()
        sliderScreenshotSimilarity.value = currentSettings.screenshotSimilarity.toFloat()

        // Обновляем текстовые метки
        textIntervalValue.text = "${currentSettings.refreshInterval} fps"
        updateVideoDurationText(currentSettings.videoDuration)
        textScreenshotSimilarityValue.text = "${currentSettings.screenshotSimilarity} %"

        when (currentSettings.videoQuality) {
            0 -> chipGroupQuality.check(R.id.chipLow)
            1 -> chipGroupQuality.check(R.id.chipMedium)
            2 -> chipGroupQuality.check(R.id.chipHigh)
            else -> chipGroupQuality.check(R.id.chipHigh)
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun setupSliders() {
        sliderInterval.addOnChangeListener { _, value, _ ->
            val safeValue = value.coerceIn(0.1f, 5.0f)
            textIntervalValue.text = String.format("%.1f", safeValue) + " fps"
            currentSettings.refreshInterval = safeValue.toDouble()
        }

        sliderVideoDuration.addOnChangeListener { _, value, _ ->
            val intValue = value.toInt()
            currentSettings.videoDuration = intValue
            updateVideoDurationText(intValue)
        }

        sliderScreenshotSimilarity.addOnChangeListener { _, value, _ ->
            val intValue = value.toInt()
            textScreenshotSimilarityValue.text = "$intValue %"
            currentSettings.screenshotSimilarity = intValue
        }
    }

    private fun updateVideoDurationText(minutes: Int) {
        textVideoDurationValue.text = when {
            minutes > 60 -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins == 0) "$hours часов"
                else "$hours часов $mins минут"
            }
            else -> "$minutes минут"
        }
    }

    private fun saveAllSettings() {
        val newToken = editTextToken.text.toString().trim()
        if (newToken.isEmpty()) {
            Toast.makeText(this, "Токен не может быть пустым", Toast.LENGTH_SHORT).show()
            return
        }

        currentSettings.token = newToken
        currentSettings.refreshInterval = sliderInterval.value.toDouble()
        currentSettings.videoDuration = sliderVideoDuration.value.toInt()
        currentSettings.screenshotSimilarity = sliderScreenshotSimilarity.value.toInt()

        settingsManager.saveSettings(currentSettings)
        settingsManager.sendSettingToDisk()

        Toast.makeText(this, "Настройки сохранены! Перезапустите стрим для применения", Toast.LENGTH_LONG).show()
    }

    private fun initializationObject() {
        sliderInterval = findViewById(R.id.sliderInterval)
        sliderVideoDuration = findViewById(R.id.sliderVideoDuration)
        sliderScreenshotSimilarity = findViewById(R.id.sliderScreenshotSimilarity)
        textIntervalValue = findViewById(R.id.textIntervalValue)
        textVideoDurationValue = findViewById(R.id.textVideoDurationValue)
        textScreenshotSimilarityValue = findViewById(R.id.textScreenshotSimilarityValue)
        buttonSaveSettings = findViewById(R.id.buttonSaveSettings)
        editTextToken = findViewById(R.id.editTextToken)
        chipGroupQuality = findViewById(R.id.chipGroupQuality)
    }
}