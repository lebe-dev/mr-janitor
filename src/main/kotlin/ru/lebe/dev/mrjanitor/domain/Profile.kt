package ru.lebe.dev.mrjanitor.domain

data class Profile(
    val name: String,

    val path: String,

    val storageUnit: StorageUnit,

    val keepCopies: Int,

    val cleanAction: CleanAction
)

enum class StorageUnit {
    FILE, DIRECTORY
}

enum class CleanAction {
    COMPRESS, REMOVE, JUST_NOTIFY
}
