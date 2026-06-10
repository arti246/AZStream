package com.example.azstream.model

sealed class ArchiveItem {
    data class Folder(val name: String, val path: String) : ArchiveItem()
    data class Video(val name: String, val path: String, val size: Long = 0) : ArchiveItem()
}