package com.example.azstream.activity

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.azstream.R

class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var videoView: VideoView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        videoView = findViewById(R.id.videoView)

        val videoUrl = intent.getStringExtra("video_url")

        if (!videoUrl.isNullOrEmpty()) {
            val uri = Uri.parse(videoUrl)
            videoView.setVideoURI(uri)

            val mediaController = MediaController(this)
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)

            videoView.start()
        } else {
            finish()
        }
    }
}