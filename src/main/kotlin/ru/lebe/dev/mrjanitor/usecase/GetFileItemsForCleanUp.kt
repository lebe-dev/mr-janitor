package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.OperationResult
import ru.lebe.dev.mrjanitor.domain.Profile
import java.nio.file.Paths

class GetFileItemsForCleanUp(
    private val createFileIndex: CreateFileIndex,
    private val checkIfFileItemValid: CheckIfFileItemValid
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getFileItems(profile: Profile): Either<OperationResult, List<FileItem>> {
        log.info("get file items for clean up for profile '${profile.name}'")
        log.info("- path: '${profile.path}'")

        return when(val fileIndex = createFileIndex.create(Paths.get(profile.path), profile.storageUnit)) {
            is Either.Right -> {
                val validatedFileItems = fileIndex.b.fileItems.map {
                    it.copy(valid = checkIfFileItemValid.isValid(it, profile.fileItemValidationConfig))
                }

                val validFilePaths = validatedFileItems.filter { it.valid }
                                                       .sortedBy { it.path.toFile().lastModified() }
                                                       .takeLast(profile.keepCopies)
                                                       .map { it.path.toString() }

                val results = validatedFileItems.filterNot { it.path.toString() in validFilePaths }

                Either.right(results)
            }
            is Either.Left -> Either.left(OperationResult.ERROR)
        }
    }
}