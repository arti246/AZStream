package com.example.azstream

import android.content.Context
import java.io.File

class TokenManager(private val context: Context) {

    private val tokenFile: File
        get() = File(context.filesDir, "yandex_token.txt")

    // Сохранить токен в файл
    fun saveToken(token: String) {
        tokenFile.writeText(token)
    }

    // Прочитать токен из файла
    fun getToken(): String {
        return if (tokenFile.exists()) {
            tokenFile.readText()
        } else {
            ""
        }
    }

    // Проверить, существует ли токен
    fun hasToken(): Boolean {
        return tokenFile.exists() && tokenFile.readText().isNotEmpty()
    }

    // Удалить токен
    fun deleteToken() {
        if (tokenFile.exists()) {
            tokenFile.delete()
        }
    }
}