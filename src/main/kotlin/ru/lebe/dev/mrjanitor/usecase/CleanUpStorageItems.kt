package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.OperationError
import ru.lebe.dev.mrjanitor.domain.OperationResult
import ru.lebe.dev.mrjanitor.domain.StorageItem
import java.nio.file.Path

class CleanUpStorageItems {
    private val log = LoggerFactory.getLogger(javaClass)

    fun cleanUp(items: List<StorageItem>): OperationResult<Boolean> = try {
        log.info("cleanup storage items (${items.size})")

        var hasErrors = false

        items.forEach { item ->
            log.info("- remove item '${item.path}'..")

            if (item.path.toFile().isDirectory) {
                val directoryDeleted = deleteDirectory(item.path)

                if (directoryDeleted) {
                    log.info("+ directory has been deleted")

                } else {
                    log.error("[!] unable to delete directory")
                    hasErrors = true
                }

            } else {
                deleteFile(item.path)
            }
        }

        if (!hasErrors) {
            log.info("all items have been removed")

        } else {
            log.error("cleanup has errors, check log for details")
        }

        Either.right(!hasErrors)

    } catch (e: Exception) {
        log.error("unable to cleanup storage items: ${e.message}", e)
        Either.left(OperationError.ERROR)
    }

    private fun deleteFile(path: Path) {
        if (path.toFile().delete()) {
            log.info("- deleted: '$path'")

        } else {
            log.error("unable to delete file item")
        }
    }

    private fun deleteDirectory(path: Path): Boolean {
        var result = false

        if (path.toFile().deleteRecursively()) {
            log.info("- path '$path' has been deleted")
            result = true

        } else {
            log.error("unable to delete item path")
        }

        return result
    }
}
