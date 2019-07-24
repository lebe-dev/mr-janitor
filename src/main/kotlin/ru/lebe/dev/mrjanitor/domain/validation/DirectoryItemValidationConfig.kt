package ru.lebe.dev.mrjanitor.domain.validation

data class DirectoryItemValidationConfig(
    /**
     * Expect directory total size >= previous directory item
     */
    val sizeAtLeastAsPrevious: Boolean,

    /**
     * Expect file item size >= same file size in previous directory
     */
    val fileSizeAtLeastAsInPrevious: Boolean,

    /**
     * Expect files quantity >= previous directory item
     */
    val filesQtyAtLeastAsInPrevious: Boolean
)
