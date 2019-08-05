package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.OperationResult
import ru.lebe.dev.mrjanitor.domain.Profile

class GetFileItemsForCleanUp(
    private val createFileIndex: CreateFileIndex,
    private val checkIfFileItemValid: CheckIfFileItemValid
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getFileItems(profile: Profile): Either<OperationResult, List<FileItem>> {
        log.info("get file items for clean up for profile '${profile.name}'")
        log.info("- path: '${profile.path}'")

        return when(val fileIndex = createFileIndex.create(profile)) {
            is Either.Right -> {
                val validatedFileItems = fileIndex.b.fileItems.map { fileItem ->

                    fileItem.copy(
                        valid = checkIfFileItemValid.isValid(
                            fileItem,
                            getPreviousFileItem(
                                fileIndex.b.fileItems, fileItem.path.toString()
                            ),
                            profile.fileItemValidationConfig
                        )
                    )
                }

                val validFilePaths = validatedFileItems.filter { it.valid }
                                                       .sortedBy { it.path.toFile().lastModified() }
                                                       .takeLast(profile.keepItemsQuantity)
                                                       .map { it.path.toString() }

                val results = validatedFileItems.filterNot { it.path.toString() in validFilePaths }

                Either.right(results)
            }
            is Either.Left -> Either.left(OperationResult.ERROR)
        }
    }

    private fun getPreviousFileItem(fileItems: List<FileItem>, fileItemPath: String): Option<FileItem> {
        val itemsBeforeCurrent = fileItems.takeWhile { it.path.toString() != fileItemPath }

        return if (itemsBeforeCurrent.isNotEmpty()) {
            Some(itemsBeforeCurrent.last())

        } else {
            None
        }
    }
}
