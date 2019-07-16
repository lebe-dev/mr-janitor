package ru.lebe.dev.mrjanitor.domain

data class FileItem(
    val name: String,
    val size: Long,
    val hash: String
)
