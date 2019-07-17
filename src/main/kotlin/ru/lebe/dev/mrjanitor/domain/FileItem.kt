package ru.lebe.dev.mrjanitor.domain

import java.nio.file.Path

data class FileItem(
    val path: Path,
    val name: String,
    val size: Long,
    val hash: String,
    val valid: Boolean
)
