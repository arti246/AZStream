package com.example.azstream.managers

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.azstream.activity.StreamActivity
import com.example.azstream.network.NetworkChecker
import com.example.azstream.yandex.YandexDiskClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import kotlin.time.Duration.Companion.milliseconds

class StreamManager(
    private val activity: StreamActivity,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val settings: SettingsData,
    private val settingsManager: SettingsManager
) {
    private var contextActivity = activity
    private var yandexDiskClient = YandexDiskClient(settingsManager)
    private var networkChecker = NetworkChecker()
    private var pollingJob: Job? = null
    private var isPolling: Boolean = false
    private var currentStation: String = "Station_1"

    // Проверка скринов
    private var lastLoadedHash: String? = null
    private var consecutiveSameImages = 0
    private val MAX_SAME_IMAGES = 3

    var onWarning: ((String) -> Unit)? = null

    suspend fun checkPreconditions(): Int = withContext(Dispatchers.IO) {
        if (isPolling) return@withContext -2
        if (!networkChecker.isConnectedToNetwork(contextActivity)) return@withContext -1
        if (!yandexDiskClient.checkDiskConnection()) return@withContext -3
        return@withContext 1
    }

    fun startPolling(stationName: String, onImageLoaded: (Bitmap) -> Unit) {
        if (isPolling) return
        isPolling = true
        currentStation = stationName

        lastLoadedHash = null
        consecutiveSameImages = 0

        val intervalMs = if (settings.refreshInterval > 0)
            (1000 / settings.refreshInterval).toLong()
        else 1000L

        pollingJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive && isPolling) {
                try {
                    val bitmap = checkAndLoadImage(currentStation)
                    if (bitmap != null) {
                        withContext(Dispatchers.Main) {
                            onImageLoaded(bitmap)
                        }
                    }
                    delay(intervalMs.milliseconds)
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(intervalMs.milliseconds)
                }
            }
            isPolling = false
        }
    }

    fun stopPolling() {
        isPolling = false
        pollingJob?.cancel()
        pollingJob = null
    }

    fun getIsPolling(): Boolean = isPolling

    fun updateStation(stationName: String) {
        currentStation = stationName
    }

    private suspend fun checkAndLoadImage(stationName: String): Bitmap? {
        val (bitmap, modifiedTime) = yandexDiskClient.getLastScreenshotWithMeta(stationName)

        if (bitmap == null) {
            consecutiveSameImages++
            if (consecutiveSameImages >= MAX_SAME_IMAGES) {
                // ✅ Показываем предупреждение через callback
                val message = "Изображение не обновлялось ${settings.refreshInterval * MAX_SAME_IMAGES} секунд!"
                withContext(Dispatchers.Main) {
                    onWarning?.invoke(message)
                }
                return null
            }
            return null
        }

        val currentHash = getBitmapHash(bitmap)

        if (currentHash == lastLoadedHash) {
            consecutiveSameImages++
            if (consecutiveSameImages >= MAX_SAME_IMAGES) {
                val intervalSeconds  = 1.0 / settings.refreshInterval
                val message = "Изображение не обновлялось ${intervalSeconds * MAX_SAME_IMAGES} секунд!"
                withContext(Dispatchers.Main) {
                    onWarning?.invoke(message)
                }
                return null
            }
            return null
        }

        lastLoadedHash = currentHash
        consecutiveSameImages = 0
        return bitmap
    }

    private fun getBitmapHash(bitmap: Bitmap): String {
        val byteArray = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArray)
        val bytes = byteArray.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return Base64.encodeToString(hashBytes, Base64.DEFAULT)
    }

    fun checkStartState(): Int {
        if (!networkChecker.isConnectedToNetwork(contextActivity)) {
            return -1
        } else if (settingsManager.getToken().isBlank()) {
            return -2
        }
        return 0
    }
}