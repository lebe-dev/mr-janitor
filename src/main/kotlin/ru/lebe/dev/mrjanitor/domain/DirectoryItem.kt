package ru.lebe.dev.mrjanitor.domain

data class DirectoryItem(
    val name: String,
    val size: Long,
    val fileItems: List<FileItem>
)
