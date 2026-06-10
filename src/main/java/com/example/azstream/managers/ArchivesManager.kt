package com.example.azstream.managers

import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.azstream.activity.ArchiveActivity
import com.example.azstream.model.ArchiveAdapter
import com.example.azstream.network.NetworkChecker
import com.example.azstream.yandex.YandexDiskClient
import kotlinx.coroutines.launch

class ArchivesManager(
    private val activity: ArchiveActivity,
    private val lifecycleScope: LifecycleCoroutineScope,
) {
    private var yandexDiskClient = YandexDiskClient(activity)
    private var networkChecker = NetworkChecker()

    public fun loadArchiveContent(currentPath: String, adapter: ArchiveAdapter) {
        lifecycleScope.launch {
            if(!networkChecker.isConnectedToNetwork(activity)) {
                Toast.makeText(activity, "Нет подключения к интернету", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                val items = yandexDiskClient.getArchiveFoldersAndVideos(currentPath)
                if (items.isNotEmpty()) {
                    adapter.updateItems(items)
                } else {
                    adapter.updateItems(emptyList())
                    Toast.makeText(activity, "Папка пуста", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun getVideoStreamUrl(videoPath: String): String? {
        return yandexDiskClient.getVideoStreamUrl(videoPath)
    }
}