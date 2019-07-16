package ru.lebe.dev.mrjanitor.usecase

import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import ru.lebe.dev.mrjanitor.domain.validation.DirectoryItemValidationConfig
import java.nio.file.Path

class CheckIfDirectoryItemValid {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isValid(directoryItem: DirectoryItem, previousDirectoryItem: DirectoryItem,
                validationConfig: DirectoryItemValidationConfig): Boolean {

        log.info("check if directory-item is valid, path '${directoryItem.path}'")

        var result = false

        if (isBasicValidationSuccess(directoryItem)) {

            if (validationConfig.qtyAtLeastAsInPreviousItem) {

                if (directoryItem.fileItems.size >= previousDirectoryItem.fileItems.size) {
                    result = true
                }

            } else {
                result = true
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
