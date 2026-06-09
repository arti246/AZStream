package com.example.azstream.yandex

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.azstream.Secrets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.*
import org.json.JSONObject

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
                .url("https://cloud-api.yandex.net/v1/disk/resources/download?path=Stream/last.png")
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
}