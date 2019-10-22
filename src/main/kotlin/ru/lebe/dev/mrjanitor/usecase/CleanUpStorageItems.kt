package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Try
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.StorageItem
import java.nio.file.Path

class CleanUpStorageItems {
    private val log = LoggerFactory.getLogger(javaClass)

    fun cleanUp(items: List<StorageItem>): Try<Unit> = Try {
        log.info("cleanup storage items (${items.size})")
        items.forEach { log.info("- '${it.name}'") }

        items.forEach { item ->
            log.info("- remove item '${item.path}'..")

            if (item.path.toFile().isDirectory) {
                deleteDirectory(item.path)

            } else {
                deleteFile(item.path)
            }
        }

        log.info("all items have been removed")
    }

    private fun deleteFile(path: Path) {
        if (path.toFile().delete()) {
            log.info("- deleted: '$path'")

        } else {
            log.error("unable to delete file item")
        }
    }

    private fun deleteDirectory(path: Path) {
        if (path.toFile().deleteRecursively()) {
            log.info("- path '$path' has been deleted")

        } else {
            log.error("unable to delete item path")
        }
    }
}
