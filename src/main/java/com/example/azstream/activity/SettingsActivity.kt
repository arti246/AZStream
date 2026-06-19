package com.example.azstream.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.example.azstream.R
import com.example.azstream.managers.SettingsData
import com.example.azstream.managers.SettingsManager
import com.google.android.material.slider.Slider

class SettingsActivity : BaseActivity() {
    private lateinit var sliderInterval: Slider
    private lateinit var textIntervalValue: TextView
    private lateinit var buttonSaveSettings: Button
    private lateinit var editTextToken: EditText
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

        settingsManager = SettingsManager(this)

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
    }

    private fun applySettingsToUi() {
        editTextToken.setText(currentSettings.token)
        sliderInterval.value = currentSettings.refreshInterval.toFloat()

        // Обновляем текстовые метки
        textIntervalValue.text = "${currentSettings.refreshInterval} fps"
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun setupSliders() {
        sliderInterval.addOnChangeListener { _, value, _ ->
            val safeValue = value.coerceIn(0.1f, 5.0f)
            textIntervalValue.text = String.format("%.1f", safeValue) + " fps"
            currentSettings.refreshInterval = safeValue.toDouble()
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

        settingsManager.saveSettings(currentSettings)

        Toast.makeText(this, "Настройки сохранены! Перезапустите стрим для применения", Toast.LENGTH_LONG).show()
    }

    private fun initializationObject() {
        sliderInterval = findViewById(R.id.sliderInterval)
        textIntervalValue = findViewById(R.id.textIntervalValue)
        buttonSaveSettings = findViewById(R.id.buttonSaveSettings)
        editTextToken = findViewById(R.id.editTextToken)
    }
}