package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Option
import arrow.core.getOrElse
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import ru.lebe.dev.mrjanitor.domain.FileItemValidationConfig
import ru.lebe.dev.mrjanitor.domain.validation.DirectoryItemValidationConfig
import java.nio.file.Path
import java.nio.file.Paths

class CheckIfDirectoryItemValid(
        private val checkIfFileItemValid: CheckIfFileItemValid
    ) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isValid(directoryItem: DirectoryItem, previousDirectoryItem: Option<DirectoryItem>,
                directoryValidationConfig: DirectoryItemValidationConfig,
                fileValidationConfig: FileItemValidationConfig): Boolean {

        log.info("---")
        log.info("check if directory-item is valid, path '${directoryItem.path}'")

        var result = false

        if (isBasicValidationSuccess(directoryItem)) {

            if (directoryValidationConfig.qtyAtLeastAsInPreviousItem && previousDirectoryItem.isDefined()) {

                val previousItem = previousDirectoryItem.getOrElse {
                    DirectoryItem(path = Paths.get("."), name = "", size = 0, fileItems = listOf(), valid = false)
                }

                log.debug("  - files in current directory: ${directoryItem.fileItems.size}")
                log.debug("  - files in previous directory: ${previousItem.fileItems.size}")

                if (directoryItem.fileItems.size >= previousItem.fileItems.size) {
                    log.debug("- directory contains expected file-items count: true")

                    result = directoryItem.fileItems.all {
                        checkIfFileItemValid.isValid(fileItem = it, validationConfig = fileValidationConfig)
                    }

                    log.debug("- all file items are valid: $result")

                } else {
                    log.debug("- directory contains expected file-items count: false")
                }

            } else {
                result = directoryItem.fileItems.all {
                    checkIfFileItemValid.isValid(fileItem = it, validationConfig = fileValidationConfig)
                }

                log.debug("- all file items are valid: $result")
            }
        }

        return result
    }

    private fun isBasicValidationSuccess(directoryItem: DirectoryItem): Boolean {
        var result = false

        if (isPathValid(directoryItem.path)) {

            if (directoryItem.size > 0) {

                if (directoryItem.fileItems.isNotEmpty()) {
                    result = true

                } else {
                    log.info("validation error - there are no file items in directory")
                }

            } else {
                log.info("validation error - directory size equals 0")
            }

        } else {
            log.error("validation error - path doesn't exist")
        }

        return result
    }

    private fun isPathValid(path: Path) = path.toFile().exists() && path.toFile().isDirectory
}
