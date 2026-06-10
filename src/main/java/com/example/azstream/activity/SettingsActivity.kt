package com.example.azstream.activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.example.azstream.R
import com.example.azstream.TokenManager

class SettingsActivity : BaseActivity() {
    private val prefs by lazy {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        setupDrawer() //отрисовка меню
        setupWindowInsets() //настройка padding

        var buttonSaveSettings = findViewById<Button>(R.id.buttonSaveSettings)
        var editTextToken = findViewById<EditText>(R.id.editTextToken)
        var tokenManager = TokenManager(this)

        // Загружаем токен из файла
        editTextToken.setText(tokenManager.getToken())


        buttonSaveSettings.setOnClickListener {
            val newToken = editTextToken.text.toString().trim()
            if (newToken.isNotEmpty()) {
                tokenManager.saveToken(newToken)
                Toast.makeText(this, "Токен сохранён в файл!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Токен не может быть пустым", Toast.LENGTH_SHORT).show()
            }
        }
    }
}