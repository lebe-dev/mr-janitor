package ru.lebe.dev.mrjanitor.util

import ru.lebe.dev.mrjanitor.domain.StorageUnit

object Defaults {
    const val REPORT_FILE_NAME = "janitor.report"

    const val PROFILE_NAME = "defaults"

    const val LOG_ROW_BOLD_SEPARATOR = "================================================="
    const val LOG_ROW_SEPARATOR = "-------------------------------------------------"

    const val EXIT_CODE_OK = 0

    const val FILENAME_FILTER_PATTERN = ".*"
    const val DIRECTORY_NAME_FILTER_PATTERN = "\\d{4}-\\d{2}-\\d{2}"

    val DEFAULT_STORAGE_UNIT = StorageUnit.FILE
    const val DEFAULT_KEEP_COPIES = 7
}
