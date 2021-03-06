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
import ru.lebe.dev.mrjanitor.util.Defaults.LOG_ROW_SEPARATOR
import java.nio.file.Path
import java.nio.file.Paths

class CheckIfDirectoryItemValid(
        private val checkIfFileItemValid: CheckIfFileItemValid
    ) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isValid(directoryItem: DirectoryItem, previousDirectoryItem: Option<DirectoryItem>,
                directoryValidationConfig: DirectoryItemValidationConfig,
                fileValidationConfig: FileItemValidationConfig): Boolean {

        log.info(LOG_ROW_SEPARATOR)
        log.info(" CHECK PATH '${directoryItem.path}'")
        log.info(LOG_ROW_SEPARATOR)
        log.trace(directoryItem.toString())

        log.debug("directory validation config:")
        log.debug(directoryValidationConfig.toString())

        log.debug("file validation config:")
        log.debug(fileValidationConfig.toString())

        var result = false

        if (isBasicValidationSuccess(directoryItem)) {
            var nextCheckLock = false

            if (directoryValidationConfig.filesQtyAtLeastAsInPrevious && previousDirectoryItem.isDefined()) {
                val previousItem = previousDirectoryItem.getOrElse { getInvalidDirectoryItem() }
                log.trace("previous-item:")
                log.trace(previousItem.toString())

                log.debug("  - files in current directory: ${directoryItem.fileItems.size}")
                log.debug("  - files in previous directory ('${previousItem.name}'): ${previousItem.fileItems.size}")

                if (directoryItem.fileItems.size >= previousItem.fileItems.size) {
                    log.debug("- directory contains expected file-items count: true")

                    result = true
                    log.debug("- all file items are valid: $result")

                } else {
                    log.warn("- directory contains expected file-items count: false")
                }

                if (!result) { nextCheckLock = true }
            }

            if (!nextCheckLock && directoryValidationConfig.sizeAtLeastAsPrevious &&
                previousDirectoryItem.isDefined()) {

                val previousItem = previousDirectoryItem.getOrElse { getInvalidDirectoryItem() }

                if (directoryItem.size >= previousItem.size) {
                    log.debug("- current directory size >= previous: $result")
                    result = true

                } else {
                    log.warn("- current directory '${directoryItem.name}' size ${directoryItem.size} < " +
                             "previous directory '${previousItem.name}' size ${previousItem.size}")
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
                    log.debug("- basic checks: ok")
                    result = true

                } else {
                    log.error("validation error - there are no file items in directory")
                }

            } else { log.error("validation error - directory size equals 0") }

        } else { log.error("validation error - path doesn't exist") }

        return result
    }

    private fun isPathValid(path: Path) = path.toFile().exists() && path.toFile().isDirectory

    private fun getInvalidDirectoryItem() =
        DirectoryItem(path = Paths.get("."), name = "", size = 999999999999999, fileItems = listOf(), valid = false)
}
