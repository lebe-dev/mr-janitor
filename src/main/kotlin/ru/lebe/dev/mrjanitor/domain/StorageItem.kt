package ru.lebe.dev.mrjanitor.domain

import java.nio.file.Path

interface StorageItem {
    val name: String
    val path: Path
}
