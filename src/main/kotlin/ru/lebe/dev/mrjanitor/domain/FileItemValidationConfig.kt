package ru.lebe.dev.mrjanitor.domain

data class FileItemValidationConfig(
    val md5FileCheck: Boolean,
    val zipTest: Boolean,
    val logFileExists: Boolean
)
