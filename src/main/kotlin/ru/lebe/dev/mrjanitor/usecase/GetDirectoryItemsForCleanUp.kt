package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import ru.lebe.dev.mrjanitor.domain.OperationResult
import ru.lebe.dev.mrjanitor.domain.PathFileIndex
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.util.Defaults.LOG_ROW_SEPARATOR

class GetDirectoryItemsForCleanUp(
    private val createFileIndex: CreateFileIndex,
    private val checkIfDirectoryItemValid: CheckIfDirectoryItemValid
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getItems(profile: Profile): Either<OperationResult, List<DirectoryItem>> {
        log.info(LOG_ROW_SEPARATOR)
        log.info(" PROFILE '${profile.name}'")
        log.info(LOG_ROW_SEPARATOR)
        log.info("- path: '${profile.path}'")

        return when(val fileIndex = createFileIndex.create(profile)) {
            is Either.Right -> {
                val validatedDirectoryItems = getValidatedDirectoryItems(profile, fileIndex.b)

                val validDirectoryPaths = validatedDirectoryItems.filter { it.valid }
                                                                 .sortedBy { it.name }
                                                                 .takeLast(profile.keepItemsQuantity)
                                                                 .map { it.path.toString() }

                val results = when {
                    profile.cleanUpPolicy.allInvalidItems ->
                                                        getInvalidItems(validatedDirectoryItems, validDirectoryPaths)

                    profile.cleanUpPolicy.invalidItemsBeyondOfKeepQuantity -> {
                        var validCounter = 0

                        val excludeItems = validatedDirectoryItems.takeLastWhile {
                            if (it.valid && (validCounter < profile.keepItemsQuantity)) {
                                validCounter++
                                true

                            } else {
                                validCounter < profile.keepItemsQuantity
                            }

                        }.map { it.path.toString() }

                        validatedDirectoryItems.filterNot {
                            it.path.toString() in excludeItems
                        }
                    }
                    else -> listOf()
                }

                Either.right(results)
            }
            is Either.Left -> Either.left(OperationResult.ERROR)
        }
    }

    private fun getValidatedDirectoryItems(profile: Profile, pathFileIndex: PathFileIndex): List<DirectoryItem> =
        pathFileIndex.directoryItems.map { directoryItem ->
            val previousDirectoryItem = getPreviousItem(pathFileIndex, directoryItem)

            directoryItem.copy(valid = checkIfDirectoryItemValid.isValid(
                directoryItem, previousDirectoryItem,
                profile.directoryItemValidationConfig, profile.fileItemValidationConfig)
            )
        }

    private fun getInvalidItems(directoryItems: List<DirectoryItem>,
                                validDirectoryPaths: List<String>): List<DirectoryItem> =
                                            directoryItems.filterNot { it.path.toString() in validDirectoryPaths }

    private fun getPreviousItem(pathFileIndex: PathFileIndex,
                                currentDirectoryItem: DirectoryItem): Option<DirectoryItem> {

        val lastFilteredItem = pathFileIndex.directoryItems.takeWhile { item ->
            item.path.toString() != currentDirectoryItem.path.toString()
        }.lastOrNull()

        return if (lastFilteredItem != null) {
            Some(lastFilteredItem)

        } else {
            None
        }
    }
}
