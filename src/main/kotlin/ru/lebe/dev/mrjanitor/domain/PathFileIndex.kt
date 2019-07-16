package ru.lebe.dev.mrjanitor.domain

import java.nio.file.Path

data class PathFileIndex(
    val path: Path,

    val storageUnit: StorageUnit,

    val directoryItems: List<DirectoryItem>,
    val fileItems: List<FileItem>
)
