package com.example.azstream.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.example.azstream.R

class AboutAppActivity : BaseActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about_app)
        setupDrawer()
        setupWindowInsets()
    }
}