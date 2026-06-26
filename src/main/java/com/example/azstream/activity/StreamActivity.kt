package com.example.azstream.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.azstream.R
import com.example.azstream.managers.SettingsData
import com.example.azstream.managers.SettingsManager
import com.example.azstream.managers.StreamManager
import com.example.azstream.yandex.YandexDiskClient
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.launch

class StreamActivity : BaseActivity() {
    private lateinit var streamManager: StreamManager
    private lateinit var buttonStartStream: Button
    private lateinit  var buttonEndStream: Button
    private lateinit var spinnerStations: Spinner
    private lateinit var imageStream: PhotoView
    private lateinit var textPreview: TextView

    private var currentSettings: SettingsData = SettingsData()
    private lateinit var settingsManager: SettingsManager
    private var currentStation: String = "Station_1"

    // ← Добавить эти переменные
    private var stationsList: List<String> = emptyList()
    private lateinit var yandexDiskClient: YandexDiskClient

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupDrawer()
        setupWindowInsets()

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        buttonStartStream = findViewById(R.id.buttonStartStream)
        buttonEndStream = findViewById(R.id.buttonEndStream)
        spinnerStations = findViewById(R.id.spinnerStations)
        imageStream = findViewById(R.id.ImageStream)
        textPreview = findViewById(R.id.textPreview)

        settingsManager = SettingsManager(this)
        currentSettings = settingsManager.loadSettings()

        // ← Инициализируем клиент
        yandexDiskClient = YandexDiskClient(settingsManager)
        currentStation = settingsManager.getSelectedStation()

        streamManager = StreamManager(this, lifecycleScope, currentSettings, settingsManager)

        streamManager.onWarning = { message ->
            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }

        buttonStartStream.setOnClickListener {
            lifecycleScope.launch {
                val check = streamManager.checkPreconditions()
                when (check) {
                    1 -> {
                        textPreview.isVisible = false
                        buttonStartStream.isEnabled = false
                        buttonEndStream.isEnabled = true
                        streamManager.startPolling(currentStation) { bitmap ->
                            imageStream.setImageBitmap(bitmap)
                        }
                        Toast.makeText(this@StreamActivity, "Стрим запущен!", Toast.LENGTH_SHORT).show()
                    }
                    -1 -> Toast.makeText(this@StreamActivity, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
                    -2 -> Toast.makeText(this@StreamActivity, "Стрим уже запущен", Toast.LENGTH_SHORT).show()
                    -3 -> Toast.makeText(this@StreamActivity, "Ошибка Яндекс.Диска", Toast.LENGTH_LONG).show()
                }
            }
        }

        buttonEndStream.setOnClickListener {
            closeStream()
            Toast.makeText(this, "Стрим остановлен", Toast.LENGTH_SHORT).show()
        }

        val state = streamManager.checkStartState()
        when (state) {
            -1 -> {
                Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
                buttonStartStream.isEnabled = false
            }
            -2 -> {
                Toast.makeText(this, "Не указан токен Яндекс.Диска. Перейдите в настройки", Toast.LENGTH_LONG).show()
                buttonStartStream.isEnabled = false
            }
            0 -> {
                buttonStartStream.isEnabled = true
                loadStationsList()
            }
        }
    }

    private fun loadStationsList() {
        lifecycleScope.launch {
            try {
                val stations = yandexDiskClient.getStationsList("Приложения/AZStream")
                if (stations.isNotEmpty()) {
                    stationsList = stations

                    val adapter = ArrayAdapter(
                        this@StreamActivity,
                        android.R.layout.simple_spinner_item,
                        stationsList
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerStations.adapter = adapter

                    // Восстанавливаем сохранённую станцию
                    val savedStation = settingsManager.getSelectedStation()
                    if (savedStation in stationsList) {
                        val position = stationsList.indexOf(savedStation)
                        spinnerStations.setSelection(position)
                        currentStation = savedStation
                    }

                    // Обработка выбора
                    spinnerStations.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            currentStation = stationsList[position]
                            settingsManager.saveSelectedStation(currentStation)

                            streamManager.updateStation(currentStation)

                            if (streamManager.getIsPolling()) {
                                streamManager.stopPolling()
                                streamManager.startPolling(currentStation) { bitmap ->
                                    imageStream.setImageBitmap(bitmap)
                                }
                                Toast.makeText(
                                    this@StreamActivity,
                                    "Переключено на $currentStation",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                } else {
                    Toast.makeText(this@StreamActivity, "Станции не найдены", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@StreamActivity, "Ошибка загрузки станций: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        streamManager.stopPolling()
    }

    override fun onPause() {
        super.onPause()
        // Если стрим запущен — останавливаем
        if (streamManager.getIsPolling()) {
            streamManager.stopPolling()
            closeStream()
        }
    }

    fun closeStream() {
        streamManager.stopPolling()

        buttonStartStream.isEnabled = true
        buttonEndStream.isEnabled = false
        textPreview.isVisible = true
        imageStream.setImageBitmap(null)
    }
}