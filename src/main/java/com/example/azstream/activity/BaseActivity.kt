package com.example.azstream.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.azstream.R
import com.google.android.material.navigation.NavigationView
import kotlin.system.exitProcess

abstract class BaseActivity : AppCompatActivity() {
    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun setupWindowInsets() {
        val rootView = findViewById<View>(android.R.id.content).rootView
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    protected fun setupDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        // Обработка нажатий в меню
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_stream -> {
                    // Если мы не в StreamActivity — переходим
                    if (this !is StreamActivity) {
                        startActivity(Intent(this, StreamActivity::class.java))
                        finish()
                    }
                    drawerLayout.closeDrawers()
                }
                R.id.nav_archive -> {
                    if (this !is ArchiveActivity) {
                        startActivity(Intent(this, ArchiveActivity::class.java))
                    }
                    drawerLayout.closeDrawers()
                }
                R.id.nav_settings -> {
                    if (this !is SettingsActivity) {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                }
                R.id.nav_about -> {
                    Toast.makeText(this, "О приложении\nВерсия 1.0", Toast.LENGTH_LONG).show()
                }
                R.id.nav_exit -> {
                    finishAffinity()
                    exitProcess(0)
                }
            }
            true
        }

        // Кнопка меню
        findViewById<ImageButton>(R.id.buttonMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }
}