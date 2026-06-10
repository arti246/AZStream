package com.example.azstream.yandex

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.azstream.Secrets
import com.example.azstream.model.ArchiveItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.*
import org.json.JSONObject
import java.net.URLEncoder

class YandexDiskClient {
    private val QAtoken: String = Secrets.YANDEX_DISK_TOKEN
    private val baseUrl = "https://cloud-api.yandex.net/v1/disk" // Базовый URL для API Яндекс.Диска
    private val authHeader = "OAuth $QAtoken" // Заголовок авторизации для всех запросов

    private val client = OkHttpClient()

    suspend fun checkDiskConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(baseUrl)
                    .addHeader("Authorization", authHeader)
                    .build()

                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                Log.e("YandexDisk", "Ошибка: ${e.message}")
                false
            }
        }
    }

    suspend fun getLastScreenshot(): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val downloadUrlRequest = Request.Builder()
                .url("https://cloud-api.yandex.net/v1/disk/resources/download?path=Приложения/AZStream/Stream/last.jpg")
                .addHeader("Authorization", "OAuth $QAtoken")
                .build()

            val downloadUrlResponse = client.newCall(downloadUrlRequest).execute()

            if (!downloadUrlResponse.isSuccessful) return@withContext null

            val json = downloadUrlResponse.body.string()
            val jsonObject = JSONObject(json)
            val directUrl = jsonObject.getString("href")

            return@withContext downloadScreenshot(directUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    fun downloadScreenshot(directUrl: String): Bitmap? {
        val imageRequest = Request.Builder()
            .url(directUrl)
            .build()

        val imageResponse = client.newCall(imageRequest).execute()
        val bitmap = BitmapFactory.decodeStream(imageResponse.body?.byteStream())

        return bitmap;
    }

    suspend fun getArchiveFoldersAndVideos(parentPath: String): List<ArchiveItem> = withContext(
        Dispatchers.IO) {
        try {
            val encodedPath = URLEncoder.encode(parentPath, "UTF-8")
                .replace("+", "%20")

            val request = Request.Builder()
                .url("https://cloud-api.yandex.net/v1/disk/resources?path=" +
                        "$encodedPath&limit=100&fields=_embedded.items.name,_embedded.items.type," +
                        "_embedded.items.path,_embedded.items.size")
                .addHeader("Authorization", "OAuth $QAtoken")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext emptyList()

            val json = response.body?.string() ?: return@withContext emptyList()
            val jsonObject = JSONObject(json)

            val itemsArray = jsonObject.getJSONObject("_embedded").getJSONArray("items")

            val result = mutableListOf<ArchiveItem>()

            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                val name = item.getString("name")
                val path = item.getString("path")
                val type = item.getString("type")

                when (type) {
                    "dir" -> {
                        result.add(ArchiveItem.Folder(name, path))
                    }
                    "file" -> {
                        if (name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mkv")) {
                            val size = item.optLong("size", 0)
                            result.add(ArchiveItem.Video(name, path, size))
                        }
                    }
                }
            }

            // Сортируем: папки сначала, потом видео
            result.sortWith(compareBy(
                { it is ArchiveItem.Folder },
                { it.name.lowercase() }
            ))

            return@withContext result
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getVideoStreamUrl(videoPath: String): String? = withContext(Dispatchers.IO) {
        try {
            val encodedPath = URLEncoder.encode(videoPath, "UTF-8")
                .replace("+", "%20")

            val request = Request.Builder()
                .url("https://cloud-api.yandex.net/v1/disk/resources/download?path=$encodedPath")
                .addHeader("Authorization", "OAuth $QAtoken")
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext null

            val json = response.body?.string() ?: return@withContext null
            val jsonObject = JSONObject(json)

            return@withContext jsonObject.getString("href")

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}