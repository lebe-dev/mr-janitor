package ru.lebe.dev.mrjanitor.domain

data class FileItemValidationConfig(
    /**
     * Expect current item size >= previous file item
     */
    val sizeAtLeastAsPrevious: Boolean,

    /**
     * Expect .md5 companion file and check hash
     */
    val md5FileCheck: Boolean,

    /**
     * Check zip archive integrity
     */
    val zipTest: Boolean,

    /**
     * Expect log companion file
     * Examples: archive.log or archive.zip.log
     */
    val logFileExists: Boolean
)
