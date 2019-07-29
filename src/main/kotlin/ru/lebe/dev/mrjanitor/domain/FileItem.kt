package ru.lebe.dev.mrjanitor.domain

import java.nio.file.Path

data class FileItem(
    override val path: Path,
    override val name: String,
    val size: Long,
    val hash: String,
    val valid: Boolean
): StorageItem
