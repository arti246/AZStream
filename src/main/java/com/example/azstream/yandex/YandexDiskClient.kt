package com.example.azstream.yandex

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.azstream.managers.SettingsManager
import com.example.azstream.model.ArchiveItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder

class YandexDiskClient(
    private val settingsManager: SettingsManager
) {
    private val baseUrl = "https://cloud-api.yandex.net/v1/disk" // Базовый URL для API Яндекс.Диска
    private val QAtoken: String
        get() = settingsManager.getToken()
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

    suspend fun getLastScreenshot(stationName: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val path = "Приложения/AZStream/$stationName/Stream/last.jpg"
            val encodedPath = URLEncoder.encode(path, "UTF-8").replace("+", "%20")

            val downloadUrlRequest = Request.Builder()
                .url("https://cloud-api.yandex.net/v1/disk/resources/download?path=$encodedPath")
                .addHeader("Authorization", authHeader)
                .build()

            val downloadUrlResponse = client.newCall(downloadUrlRequest).execute()

            if (!downloadUrlResponse.isSuccessful) return@withContext null

            val json = downloadUrlResponse.body?.string() ?: return@withContext null
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
                .addHeader("Authorization", authHeader)
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
                .addHeader("Authorization", authHeader)
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

    suspend fun createFolderOnDisk(folderName: String): Boolean? = withContext(Dispatchers.IO) {
        val encodedPath = URLEncoder.encode("Приложения/AZStream/$folderName", "UTF-8")
            .replace("+", "%20")
        val url = "https://cloud-api.yandex.net/v1/disk/resources?path=$encodedPath"

        val content = ByteArray(0)
        val request = Request.Builder()
            .url(url)
            .put(content.toRequestBody(null, 0, content.size))
            .addHeader("Authorization", authHeader)
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()

            // 201 CREATED — стандартный ответ для успешного создания ресурса
            // 409 CONFLICT — папка уже существует
            when (response.code) {
                201 -> {
                    println("Папка '$folderName' успешно создана.")
                    true
                }
                409 -> {
                    println("Папка '$folderName' уже существует.")
                    true
                }
                else -> {
                    // Если произошла другая ошибка
                    val errorBody = response.body?.string()
                    println("Ошибка создания папки: ${response.code} $errorBody")
                    false
                }
            }
        } catch (e: Exception) {
            println("Исключение при создании папки: ${e.message}")
            false
        }
    }

    // Загрузка файла на Яндекс.Диск
    suspend fun uploadFileToDisk(localFile: File, remotePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Получаем URL для загрузки
            val encodedPath = URLEncoder.encode(remotePath, "UTF-8")
                .replace("+", "%20")

            val getUploadUrlRequest = Request.Builder()
                .url("https://cloud-api.yandex.net/v1/disk/resources/upload?path=$encodedPath&overwrite=true")
                .addHeader("Authorization", "OAuth $QAtoken")
                .build()

            val uploadUrlResponse = client.newCall(getUploadUrlRequest).execute()

            if (!uploadUrlResponse.isSuccessful) {
                Log.e("YandexDisk", "Ошибка получения URL для загрузки: ${uploadUrlResponse.code}")
                return@withContext false
            }

            val json = uploadUrlResponse.body?.string() ?: return@withContext false
            val jsonObject = JSONObject(json)
            val uploadUrl = jsonObject.getString("href")

            // 2. Загружаем файл
            val fileBody = localFile.readBytes()
            val uploadRequest = Request.Builder()
                .url(uploadUrl)
                .put(RequestBody.create(null, fileBody))
                .build()

            val uploadResponse = client.newCall(uploadRequest).execute()

            return@withContext uploadResponse.isSuccessful

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    // Скачивание файла с Яндекс.Диска
    suspend fun downloadFileFromDisk(remotePath: String, localFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Получаем URL для скачивания
            val encodedPath = URLEncoder.encode(remotePath, "UTF-8")
                .replace("+", "%20")

            val getDownloadUrlRequest = Request.Builder()
                .url("https://cloud-api.yandex.net/v1/disk/resources/download?path=$encodedPath")
                .addHeader("Authorization", "OAuth $QAtoken")
                .build()

            val downloadUrlResponse = client.newCall(getDownloadUrlRequest).execute()

            if (!downloadUrlResponse.isSuccessful) {
                Log.e("YandexDisk", "Ошибка получения URL для скачивания: ${downloadUrlResponse.code}")
                return@withContext false
            }

            val json = downloadUrlResponse.body?.string() ?: return@withContext false
            val jsonObject = JSONObject(json)
            val downloadUrl = jsonObject.getString("href")

            // 2. Скачиваем файл
            val downloadRequest = Request.Builder()
                .url(downloadUrl)
                .build()

            val downloadResponse = client.newCall(downloadRequest).execute()

            if (!downloadResponse.isSuccessful) {
                return@withContext false
            }

            downloadResponse.body?.byteStream()?.use { inputStream ->
                localFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            return@withContext true

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun getStationsList(basePath: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val encodedPath = URLEncoder.encode(basePath, "UTF-8").replace("+", "%20")
            val request = Request.Builder()
                .url("https://cloud-api.yandex.net/v1/disk/resources?path=$encodedPath&limit=100&fields=_embedded.items.name,_embedded.items.type")
                .addHeader("Authorization", authHeader)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("YandexDisk", "Ошибка получения списка станций: ${response.code}")
                return@withContext emptyList()
            }

            val json = response.body?.string() ?: return@withContext emptyList()
            val jsonObject = JSONObject(json)
            val itemsArray = jsonObject.getJSONObject("_embedded").getJSONArray("items")

            val stations = mutableListOf<String>()
            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.getJSONObject(i)
                if (item.getString("type") == "dir") {
                    stations.add(item.getString("name"))
                }
            }
            return@withContext stations.sorted()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
}