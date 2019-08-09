package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import ru.lebe.dev.mrjanitor.domain.OperationError
import ru.lebe.dev.mrjanitor.domain.PathFileIndex
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.util.Defaults.LOG_ROW_SEPARATOR

class GetDirectoryItemsForCleanUp(
    private val createFileIndex: CreateFileIndex,
    private val checkIfDirectoryItemValid: CheckIfDirectoryItemValid
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getItems(profile: Profile): Either<OperationError, List<DirectoryItem>> {
        log.info(LOG_ROW_SEPARATOR)
        log.info(" PROFILE '${profile.name}'")
        log.info(LOG_ROW_SEPARATOR)
        log.info("- path: '${profile.path}'")

        return if (profile.keepItemsQuantity > 0) {

            when(val fileIndex = createFileIndex.create(profile)) {
                is Either.Right -> {
                    val validatedDirectoryItems = getValidatedItems(profile, fileIndex.b)

                    val validDirectoryPaths = validatedDirectoryItems.filter { it.valid }
                                                                     .sortedBy { it.name }
                                                                     .takeLast(profile.keepItemsQuantity)
                                                                     .map { it.path.toString() }

                    val results = when {
                        profile.cleanUpPolicy.allInvalidItems ->
                                                        getInvalidItems(validatedDirectoryItems, validDirectoryPaths)

                        profile.cleanUpPolicy.invalidItemsBeyondOfKeepQuantity ->
                                    getInvalidItemsBeyondKeepRange(validatedDirectoryItems, profile.keepItemsQuantity)

                        else -> listOf()
                    }

                    Either.right(results)
                }
                is Either.Left -> Either.left(OperationError.ERROR)
            }

        } else {
            log.error("misconfiguration - keep-items-quantity equals zero")
            Either.left(OperationError.MISCONFIGURATION)
        }
    }

    private fun getInvalidItemsBeyondKeepRange(items: List<DirectoryItem>,
                                               keepItemsQuantity: Int): List<DirectoryItem> {
        var lastValidItemsFound = 0

        val excludeItems = items.takeLastWhile {
            if (isValidItemInKeepRange(it, keepItemsQuantity, lastValidItemsFound)) {
                lastValidItemsFound++
                true

            } else {
                lastValidItemsFound < keepItemsQuantity
            }

        }.map { it.path.toString() }

        return items.filterNot { it.path.toString() in excludeItems }
    }

    private fun isValidItemInKeepRange(item: DirectoryItem, keepItemsQuantity: Int, currentItemQuantity: Int): Boolean =
                                                            item.valid && (currentItemQuantity < keepItemsQuantity)

    private fun getValidatedItems(profile: Profile, pathFileIndex: PathFileIndex): List<DirectoryItem> =
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
