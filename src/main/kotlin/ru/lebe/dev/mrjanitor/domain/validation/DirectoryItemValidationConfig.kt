package ru.lebe.dev.mrjanitor.domain.validation

data class DirectoryItemValidationConfig(
    val fileSizeAtLeastAsPrevious: Boolean,
    val qtyAtLeastAsInPreviousItem: Boolean
)
