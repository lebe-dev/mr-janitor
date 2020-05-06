package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.OperationError
import ru.lebe.dev.mrjanitor.domain.OperationResult
import ru.lebe.dev.mrjanitor.domain.PathFileIndex
import ru.lebe.dev.mrjanitor.domain.Profile

class GetFileItemsForCleanUp(
    private val createFileIndex: CreateFileIndex,
    private val checkIfFileItemValid: CheckIfFileItemValid
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getItems(profile: Profile): OperationResult<List<FileItem>> {
        log.info("get file items for clean up for profile '${profile.name}'")
        log.info("- path: '${profile.path}'")

        return if (profile.keepItemsQuantity > 0) {

            when(val fileIndex = createFileIndex.create(profile)) {
                is Either.Right -> {
                    val validatedFileItems = getValidatedItems(profile, fileIndex.b)

                    val validFilePaths = validatedFileItems.filter { it.valid }
                        .sortedBy { it.path.toFile().lastModified() }
                        .takeLast(profile.keepItemsQuantity)
                        .map { it.path.toString() }

                    val results = when {
                        profile.cleanUpPolicy.allInvalidItems -> getAllInvalidItems(validatedFileItems, validFilePaths)

                        profile.cleanUpPolicy.invalidItemsBeyondOfKeepQuantity ->
                                          getInvalidItemsBeyondKeepRange(validatedFileItems, profile.keepItemsQuantity)
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

    private fun getValidatedItems(profile: Profile, pathFileIndex: PathFileIndex): List<FileItem> =
        pathFileIndex.fileItems.map { fileItem ->
            fileItem.copy(
                valid = checkIfFileItemValid.isValid(
                    fileItem = fileItem,
                    previousFileItem = getPreviousFileItem(pathFileIndex.fileItems, fileItem.path.toString()),
                    validationConfig = profile.fileItemValidationConfig
                )
            )
        }

    private fun getAllInvalidItems(fileItems: List<FileItem>, validFilePaths: List<String>): List<FileItem> =
                                                        fileItems.filterNot { it.path.toString() in validFilePaths }

    private fun getInvalidItemsBeyondKeepRange(fileItems: List<FileItem>, keepItemsQuantity: Int): List<FileItem> {
        var lastValidItemsFound = 0

        val excludeItems = fileItems.takeLastWhile {
            if (isValidItemInKeepRange(it, keepItemsQuantity, lastValidItemsFound)) {
                lastValidItemsFound++
                true

            } else {
                lastValidItemsFound < keepItemsQuantity
            }

        }.map { it.path.toString() }

        return fileItems.filterNot { it.path.toString() in excludeItems }
    }

    private fun isValidItemInKeepRange(fileItem: FileItem, keepItemsQuantity: Int, currentItemQuantity: Int): Boolean =
                                                            fileItem.valid && (currentItemQuantity < keepItemsQuantity)

    private fun getPreviousFileItem(fileItems: List<FileItem>, fileItemPath: String): Option<FileItem> {
        val itemsBeforeCurrent = fileItems.takeWhile { it.path.toString() != fileItemPath }

        return if (itemsBeforeCurrent.isNotEmpty()) {
            Some(itemsBeforeCurrent.last())

        } else {
            None
        }
    }
}
