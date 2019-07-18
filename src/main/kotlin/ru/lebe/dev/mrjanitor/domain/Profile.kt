package ru.lebe.dev.mrjanitor.domain

import ru.lebe.dev.mrjanitor.domain.validation.DirectoryItemValidationConfig

data class Profile(
    val name: String,

    val path: String,

    val storageUnit: StorageUnit,

    val fileNameFilter: Regex,

    val keepCopies: Int,

    val fileItemValidationConfig: FileItemValidationConfig,
    val directoryItemValidationConfig: DirectoryItemValidationConfig,

    val cleanAction: CleanAction
)

enum class StorageUnit {
    FILE, DIRECTORY
}

enum class CleanAction {
    COMPRESS, REMOVE, JUST_NOTIFY
}
