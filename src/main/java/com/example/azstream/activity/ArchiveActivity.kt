package com.example.azstream.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.azstream.R
import com.example.azstream.managers.ArchivesManager
import com.example.azstream.managers.SettingsManager
import com.example.azstream.model.ArchiveAdapter
import com.example.azstream.model.ArchiveItem
import kotlinx.coroutines.launch

class ArchiveActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ArchiveAdapter
    private lateinit var archivesManager: ArchivesManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var buttonBack: ImageButton

    private val navigationStack = mutableListOf<String>()
    private var currentStation: String = "Station_1"
    private var currentPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_archive)
        setupDrawer()
        setupWindowInsets()

        settingsManager = SettingsManager(this, this.lifecycleScope)

        // ✅ Загружаем текущую станцию из настроек
        currentStation = settingsManager.getSelectedStation()

        // ✅ Строим путь к архиву выбранной станции
        currentPath = "Приложения/AZStream/$currentStation/Archives"

        archivesManager = ArchivesManager(this, lifecycleScope, settingsManager)
        recyclerView = findViewById(R.id.recyclerViewArchive)
        buttonBack = findViewById(R.id.buttonBack)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Кнопка "Назад"
        buttonBack.setOnClickListener {
            if (navigationStack.isNotEmpty()) {
                currentPath = navigationStack.removeAt(navigationStack.size - 1)
                archivesManager.loadArchiveContent(currentPath, adapter)
                updateNavigationButtons()
            }
        }

        adapter = ArchiveAdapter(emptyList()) { item ->
            when (item) {
                is ArchiveItem.Folder -> {
                    navigationStack.add(currentPath)
                    currentPath = item.path
                    archivesManager.loadArchiveContent(currentPath, adapter)
                    updateNavigationButtons()
                }
                is ArchiveItem.Video -> {
                    lifecycleScope.launch {
                        try {
                            val videoUrl = archivesManager.getVideoStreamUrl(item.path)
                            if (videoUrl != null && videoUrl != "") {
                                val intent = Intent(this@ArchiveActivity, VideoPlayerActivity::class.java)
                                intent.putExtra("video_url", videoUrl)
                                startActivity(intent)
                            } else {
                                Toast.makeText(this@ArchiveActivity, "Не удалось получить ссылку на видео", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@ArchiveActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        recyclerView.adapter = adapter

        archivesManager.loadArchiveContent(currentPath, adapter)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navigationStack.isNotEmpty()) {
                    currentPath = navigationStack.removeAt(navigationStack.size - 1)
                    archivesManager.loadArchiveContent(currentPath, adapter)
                    buttonBack.visibility = if (navigationStack.isEmpty()) View.GONE else View.VISIBLE
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun updateNavigationButtons() {
        buttonBack.visibility = if (navigationStack.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        // Проверяем, не изменилась ли станция
        val newStation = settingsManager.getSelectedStation()
        if (newStation != currentStation) {
            currentStation = newStation
            // Обновляем путь и перезагружаем содержимое
            currentPath = "Приложения/AZStream/$currentStation/Archives"
            navigationStack.clear()  // Очищаем историю навигации
            archivesManager.loadArchiveContent(currentPath, adapter)
            updateNavigationButtons()
            Toast.makeText(this, "Станция обновлена: $currentStation", Toast.LENGTH_SHORT).show()
        }
    }
}