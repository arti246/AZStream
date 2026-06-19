package com.example.azstream.managers

import android.content.Context
import org.json.JSONObject
import java.io.File

class SettingsManager (private val context: Context) {
    private val settingsFile: File
        get() = File(context.filesDir, "settings.json")

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
                selectedStation = json.optString("selectedStation", "Station_1")
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
            put("selectedStation", settings.selectedStation)
        }
        settingsFile.writeText(json.toString())
    }

    // Получить токен
    fun getToken(): String {
        return loadSettings().token
    }

    fun getSelectedStation(): String = loadSettings().selectedStation
    fun saveSelectedStation(station: String) {
        val settings = loadSettings()
        settings.selectedStation = station
        saveSettings(settings)
    }
}

// Класс для хранения настроек
data class SettingsData(
    var token: String = "",
    var refreshInterval: Double = 0.5,
    var selectedStation: String = "Station_1"
)