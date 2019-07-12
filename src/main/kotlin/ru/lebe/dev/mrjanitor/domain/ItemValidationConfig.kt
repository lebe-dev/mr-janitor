package ru.lebe.dev.mrjanitor.domain

data class ItemValidationConfig(
    val md5FileCheck: Boolean,
    val zipTest: Boolean,
    val logFileExists: Boolean,
    val qtyAtLeastAsPreviousValid: Boolean
)
