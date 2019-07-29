package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Try
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.DirectoryItem

class CleanUpDirectoryItems {
    private val log = LoggerFactory.getLogger(javaClass)

    fun cleanUp(items: List<DirectoryItem>): Try<Unit> = Try {
        log.info("cleanup directory items (${items.size})")
        items.forEach { log.info("- '${it.name}'") }

        items.forEach { item ->
            log.info("- remove item '${item.path}'..")

            if (item.path.toFile().exists()) {
                item.path.toFile().deleteRecursively()
                log.info("- deleted: '${item.path}'")

            } else {
                log.warn("- item path doesn't exist")
            }
        }

        log.info("all items have been removed")
    }
}