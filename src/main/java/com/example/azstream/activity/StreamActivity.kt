package com.example.azstream

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.azstream.stream.StreamManager
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class StreamActivity : AppCompatActivity() {
    private lateinit var streamManager: StreamManager
    private lateinit var buttonStartStream: Button
    private lateinit  var buttonEndStream: Button
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var buttonMenu: ImageButton

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        buttonMenu = findViewById(R.id.buttonMenu)
        buttonStartStream = findViewById(R.id.buttonStartStream)
        buttonEndStream = findViewById(R.id.buttonEndStream)
        val imageStream = findViewById<PhotoView>(R.id.ImageStream)
        val textPreview = findViewById<TextView>(R.id.textPreview)

        streamManager = StreamManager(this, lifecycleScope)

        buttonMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        buttonStartStream.setOnClickListener {
            lifecycleScope.launch {
                val check = streamManager.checkPreconditions()
                when (check) {
                    1 -> {
                        textPreview.isVisible = false
                        buttonStartStream.isEnabled = false
                        buttonEndStream.isEnabled = true
                        streamManager.startPolling { bitmap ->
                            imageStream.setImageBitmap(bitmap)
                        }
                        Toast.makeText(this@StreamActivity, "Стрим запущен!", Toast.LENGTH_SHORT).show()
                    }
                    -1 -> Toast.makeText(this@StreamActivity, "Нет интернета", Toast.LENGTH_LONG).show()
                    -2 -> Toast.makeText(this@StreamActivity, "Стрим уже запущен", Toast.LENGTH_SHORT).show()
                    -3 -> Toast.makeText(this@StreamActivity, "Ошибка Яндекс.Диска", Toast.LENGTH_LONG).show()
                }
            }
        }

        buttonEndStream.setOnClickListener {
            streamManager.stopPolling()

            buttonStartStream.isEnabled = true
            buttonEndStream.isEnabled = false
            textPreview.isVisible = true
            imageStream.setImageBitmap(null)

            Toast.makeText(this, "Стрим остановлен", Toast.LENGTH_SHORT).show()
        }

        // Обработка нажатий в меню
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_stream -> {
                    Toast.makeText(this, "Просмотр стрима", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawers()
                }
                R.id.nav_archive -> {
                    Toast.makeText(this, "Просмотр архивов", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawers()
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Настройки", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawers()
                }
                R.id.nav_about -> {
                    Toast.makeText(this, "О приложении\nВерсия 1.0", Toast.LENGTH_LONG).show()
                    drawerLayout.closeDrawers()
                }
                R.id.nav_help -> {
                    Toast.makeText(this, "Помощь\n1. Нажмите Старт\n2. Наслаждайтесь", Toast.LENGTH_LONG).show()
                    drawerLayout.closeDrawers()
                }
                R.id.nav_exit -> {
                    finishAffinity()  // Закрыть приложение
                }
            }
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Останавливаем стрим при закрытии Activity
        streamManager.stopPolling()
    }

    fun returnButtonEnabled() {
        var buttonStartStream = findViewById<Button>(R.id.buttonStartStream)
        var buttonEndStream = findViewById<Button>(R.id.buttonEndStream)

        buttonStartStream.isEnabled = true
        buttonEndStream.isEnabled = false
    }
}