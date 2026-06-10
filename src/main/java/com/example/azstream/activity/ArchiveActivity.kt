package com.example.azstream.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.example.azstream.R

class ArchiveActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_archive)
        setupDrawer()
        setupWindowInsets()
    }
}