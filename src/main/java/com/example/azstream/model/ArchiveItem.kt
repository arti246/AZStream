package com.example.azstream.model

sealed class ArchiveItem {
    abstract val name: String
    data class Folder(
        override val name: String,  // override означает "переопределяю"
        val path: String
    ) : ArchiveItem()
    data class Video(
        override val name: String,  // override означает "переопределяю"
        val path: String,
        val size: Long = 0
    ) : ArchiveItem()
}