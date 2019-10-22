package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Try
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.StorageItem
import java.nio.file.Path

class CleanUpStorageItems {
    private val log = LoggerFactory.getLogger(javaClass)

    fun cleanUp(items: List<StorageItem>): Try<Unit> = Try {
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
