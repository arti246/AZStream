package com.example.azstream.managers

import android.graphics.Bitmap
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

    suspend fun checkPreconditions(): Int = withContext(Dispatchers.IO) {
        if (isPolling) return@withContext -2
        if (!networkChecker.isConnectedToNetwork(contextActivity)) return@withContext -1
        if (!yandexDiskClient.checkDiskConnection()) return@withContext -3
        return@withContext 1  // всё ок
    }
    fun startPolling(onImageLoaded: (Bitmap) -> Unit) {
        if (isPolling) return
        isPolling = true

        val intervalMs = if (settings.refreshInterval > 0)
            (1000 / settings.refreshInterval).toLong()
        else
            1000L

        pollingJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive && isPolling) {
                try {
                    val bitmap = yandexDiskClient.getLastScreenshot()
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
        pollingJob?.cancel()  // ← отменяем корутину, цикл прекращается
        pollingJob = null
    }

    fun getIsPolling(): Boolean { return isPolling}
}