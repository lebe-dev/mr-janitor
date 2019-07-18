package ru.lebe.dev.mrjanitor.usecase

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import ru.lebe.dev.mrjanitor.domain.FileItem
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
        log.debug(directoryItem.toString())

        var result = false

        if (isBasicValidationSuccess(directoryItem)) {

            var nextCheckLock = false

            if (directoryValidationConfig.qtyAtLeastAsInPreviousItem && previousDirectoryItem.isDefined()) {

                val previousItem = previousDirectoryItem.getOrElse { getInvalidDirectoryItem() }

                log.debug("previous-item:")
                log.debug(previousItem.toString())

                log.debug("  - files in current directory: ${directoryItem.fileItems.size}")
                log.debug("  - files in previous directory: ${previousItem.fileItems.size}")

                if (directoryItem.fileItems.size >= previousItem.fileItems.size) {
                    log.debug("- directory contains expected file-items count: true")

                    result = true
                    log.debug("- all file items are valid: $result")

                } else {
                    log.debug("- directory contains expected file-items count: false")
                }

                if (!result) { nextCheckLock = true }

            }

            if (!nextCheckLock && directoryValidationConfig.fileSizeAtLeastAsPrevious) {
                val previousItem = previousDirectoryItem.getOrElse { getInvalidDirectoryItem() }

                if (directoryItem.size >= previousItem.size) {
                    result = true
                }

                if (!result) { nextCheckLock = true }
            }

            if (!nextCheckLock) {

                result = directoryItem.fileItems.all { fileItem ->

                    val previousFileItem: Option<FileItem> = getPreviousFileItem(
                        directoryItem.fileItems, fileItem.path.toString()
                    )

                    checkIfFileItemValid.isValid(
                        fileItem = fileItem, previousFileItem = previousFileItem,
                        validationConfig = fileValidationConfig
                    )
                }

                log.debug("- all file items are valid: $result")

            }

        } else {
            log.error("basic validation error")
        }

        return result
    }

    private fun getPreviousFileItem(fileItems: List<FileItem>, fileItemPath: String): Option<FileItem> {
        val itemsBeforeCurrent = fileItems.takeWhile { it.path.toString() != fileItemPath }

        return if (itemsBeforeCurrent.isNotEmpty()) {
            Some(itemsBeforeCurrent.last())

        } else {
            None
        }
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

    private fun getInvalidDirectoryItem() =
        DirectoryItem(path = Paths.get("."), name = "", size = 999999999999999, fileItems = listOf(), valid = false)
}
