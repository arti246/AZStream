package com.example.azstream.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.azstream.R
import com.example.azstream.managers.SettingsData
import com.example.azstream.managers.SettingsManager
import com.example.azstream.managers.StreamManager
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.launch

class StreamActivity : BaseActivity() {
    private lateinit var streamManager: StreamManager
    private lateinit var buttonStartStream: Button
    private lateinit  var buttonEndStream: Button

    private var currentSettings: SettingsData = SettingsData()
    private lateinit var settingsManager: SettingsManager

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupDrawer() //отрисовка меню
        setupWindowInsets() //настройка padding

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        buttonStartStream = findViewById(R.id.buttonStartStream)
        buttonEndStream = findViewById(R.id.buttonEndStream)
        val imageStream = findViewById<PhotoView>(R.id.ImageStream)
        val textPreview = findViewById<TextView>(R.id.textPreview)

        settingsManager = SettingsManager(this, lifecycleScope)
        currentSettings = settingsManager.loadSettings()
        streamManager = StreamManager(this, lifecycleScope, currentSettings, settingsManager)

        buttonStartStream.setOnClickListener {
            lifecycleScope.launch {
                val check = streamManager.checkPreconditions()
                when (check) {
                    1 -> {
                        textPreview.isVisible = false
                        buttonStartStream.isEnabled = false
                        buttonEndStream.isEnabled = true
                        streamManager.startPolling { bitmap ->
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
            streamManager.stopPolling()

            buttonStartStream.isEnabled = true
            buttonEndStream.isEnabled = false
            textPreview.isVisible = true
            imageStream.setImageBitmap(null)

            Toast.makeText(this, "Стрим остановлен", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Останавливаем стрим при закрытии Activity
        streamManager.stopPolling()
    }
}