package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import ru.lebe.dev.mrjanitor.domain.OperationResult
import ru.lebe.dev.mrjanitor.domain.Profile
import java.nio.file.Paths

class GetDirectoryItemsForCleanUp(
    private val createFileIndex: CreateFileIndex,
    private val checkIfDirectoryItemValid: CheckIfDirectoryItemValid
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getItems(profile: Profile): Either<OperationResult, List<DirectoryItem>> {
        log.info("get directory items for clean up for profile '${profile.name}'")
        log.info("- path: '${profile.path}'")

        return when(val fileIndex = createFileIndex.create(
                Paths.get(profile.path), profile.storageUnit, profile.fileNameFilter)
            ) {
            is Either.Right -> {
                val validatedDirectoryItems = fileIndex.b.directoryItems.map { directoryItem ->

                    val lastFilteredItem = fileIndex.b.directoryItems.takeWhile { item ->
                        item.path.toString() != directoryItem.path.toString()
                    }.lastOrNull()

                    val previousDirectoryItem: Option<DirectoryItem> = if (lastFilteredItem != null) {
                        Some(lastFilteredItem)

                    } else {
                        None
                    }

                    directoryItem.copy(valid = checkIfDirectoryItemValid.isValid(
                        directoryItem, previousDirectoryItem,
                        profile.directoryItemValidationConfig, profile.fileItemValidationConfig)
                    )

                }

                val validDirectoryPaths = validatedDirectoryItems.filter { it.valid }
                                                                 .sortedBy { it.path.toFile().lastModified() }
                                                                 .takeLast(profile.keepCopies)
                                                                 .map { it.path.toString() }

                val results = validatedDirectoryItems.filterNot { it.path.toString() in validDirectoryPaths }

                Either.right(results)
            }
            is Either.Left -> Either.left(OperationResult.ERROR)
        }
    }
}
