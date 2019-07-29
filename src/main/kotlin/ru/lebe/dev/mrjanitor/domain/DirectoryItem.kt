package ru.lebe.dev.mrjanitor.domain

import java.nio.file.Path

data class DirectoryItem(
    override val path: Path,
    override val name: String,
    val size: Long,
    val fileItems: List<FileItem>,
    val valid: Boolean
): StorageItem
