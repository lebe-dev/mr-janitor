package ru.lebe.dev.mrjanitor.domain

import java.nio.file.Path

data class DirectoryItem(
    val path: Path,
    val name: String,
    val size: Long,
    val fileItems: List<FileItem>,
    val valid: Boolean
)
