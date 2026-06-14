package com.example.azstream.managers

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.azstream.network.NetworkChecker
import com.example.azstream.yandex.YandexDiskClient
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class SettingsManager (
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
) {
    private val yandexDiskClient = YandexDiskClient(this)
    private val networkChecker = NetworkChecker()
    private val settingsFile: File
        get() = File(context.filesDir, "settings.json")

    fun sendSettingToDisk() {
        lifecycleScope.launch {
            if (!networkChecker.isConnectedToNetwork(context)) {
                Toast.makeText(context, "Нет подключения к интернету", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                // 1. Создаём папку Settings (если её нет)
                val folderCreated = yandexDiskClient.createFolderOnDisk("Settings")
                if (folderCreated == false) {
                    Toast.makeText(context, "Проблема с созданием папки на Яндекс.Диске", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 2. Сохраняем текущие настройки в файл перед отправкой
                val currentSettings = loadSettings()
                saveSettings(currentSettings)  // гарантируем, что файл свежий

                // 3. Загружаем файл на диск
                val uploadSuccess = yandexDiskClient.uploadFileToDisk(
                    settingsFile,
                    "Приложения/AZStream/Settings/settings.json"
                )

                if (uploadSuccess) {
                    Toast.makeText(context, "Настройки отправлены на диск!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Ошибка при отправке настроек", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Загрузить все настройки
    fun loadSettings(): SettingsData {
        if (!settingsFile.exists()) {
            return SettingsData()
        }

        return try {
            val jsonString = settingsFile.readText()
            val json = JSONObject(jsonString)
            SettingsData(
                token = json.optString("token", ""),
                refreshInterval = json.optDouble("refresh_interval", 0.5).coerceIn(0.1, 5.0),
                videoDuration = json.optInt("video_duration", 5),
                screenshotSimilarity = json.optInt("screenshot_similarity", 30),
                videoQuality = json.optInt("video_quality", 1)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            SettingsData()
        }
    }

    // Сохранить все настройки
    fun saveSettings(settings: SettingsData) {
        val json = JSONObject().apply {
            put("token", settings.token)
            put("refresh_interval", settings.refreshInterval)
            put("video_duration", settings.videoDuration)
            put("screenshot_similarity", settings.screenshotSimilarity)
            put("video_quality", settings.videoQuality)
        }
        settingsFile.writeText(json.toString())
    }

    // Обновить только токен
    fun saveToken(token: String) {
        val settings = loadSettings()
        settings.token = token
        saveSettings(settings)
    }

    // Получить токен
    fun getToken(): String {
        return loadSettings().token
    }
}

// Класс для хранения настроек
data class SettingsData(
    var token: String = "",
    var refreshInterval: Double = 0.5,
    var videoDuration: Int = 10,
    var screenshotSimilarity: Int = 30,
    var videoQuality: Int = 2  // 0=Low, 1=Medium, 2=High
)