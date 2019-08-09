package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.lebe.dev.mrjanitor.domain.CleanAction
import ru.lebe.dev.mrjanitor.domain.CleanUpPolicy
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.FileItemValidationConfig
import ru.lebe.dev.mrjanitor.domain.OperationError
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.domain.validation.DirectoryItemValidationConfig
import ru.lebe.dev.mrjanitor.util.Defaults
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createFilesWithAbsentHashFile
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createFilesWithAbsentLogFile
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createFilesWithInvalidHash
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createInvalidArchiveFiles
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createValidArchiveFiles
import ru.lebe.dev.mrjanitor.util.assertErrorResult
import ru.lebe.dev.mrjanitor.util.assertRightResult
import java.nio.file.Files
import java.nio.file.Path

internal class GetFileItemsForCleanUpTest {

    private lateinit var indexPath: Path

    private val createFileIndex = CreateFileIndex()
    private val checkIfFileItemValid = CheckIfFileItemValid()

    private lateinit var useCase: GetFileItemsForCleanUp

    private lateinit var profile: Profile

    private val fileItemValidationConfig = FileItemValidationConfig(
        sizeAtLeastAsPrevious = true,
        md5FileCheck = true, zipTest = true, logFileExists = true,
        useCustomValidator = false, customValidatorCommand = ""
    )

    private val directoryItemValidationConfig = DirectoryItemValidationConfig(
        sizeAtLeastAsPrevious = true,
        filesQtyAtLeastAsInPrevious = true, fileSizeAtLeastAsInPrevious = true
    )

    @BeforeEach
    fun setUp() {
        indexPath = Files.createTempDirectory("")

        profile = Profile(
            name = "test",
            path = indexPath.toString(),
            storageUnit = StorageUnit.FILE,
            fileNameFilter = Regex(Defaults.FILENAME_FILTER_PATTERN),
            directoryNameFilter = Regex(Defaults.DIRECTORY_NAME_FILTER_PATTERN),
            keepItemsQuantity = 5,
            fileItemValidationConfig = fileItemValidationConfig,
            directoryItemValidationConfig = directoryItemValidationConfig,
            cleanUpPolicy = CleanUpPolicy(invalidItemsBeyondOfKeepQuantity = false, allInvalidItems = true),
            cleanAction = CleanAction.JUST_NOTIFY
        )

        useCase = GetFileItemsForCleanUp(createFileIndex, checkIfFileItemValid)
    }

    @AfterEach
    fun tearDown() {
        indexPath.toFile().deleteRecursively()
    }

    @Test
    fun `Result list should be equal to keep-copies value`() {
        createValidArchiveFiles(indexPath, 2)

        createFilesWithInvalidHash(indexPath, 1)
        createFilesWithAbsentHashFile(indexPath, 1)

        createValidArchiveFiles(indexPath, 2) // GOOD

        createInvalidArchiveFiles(indexPath, 2)

        createValidArchiveFiles(indexPath, 2) // GOOD

        createFilesWithAbsentHashFile(indexPath, 3)
        createFilesWithAbsentLogFile(indexPath, 2)

        createValidArchiveFiles(indexPath, 1) // GOOD

        createInvalidArchiveFiles(indexPath, 2)
        createFilesWithInvalidHash(indexPath, 2)

        createValidArchiveFiles(indexPath, 2) // GOOD

        val results = useCase.getFileItems(profile)

        assertTrue(results.isRight())

        when(results) {
            is Either.Right -> {
                val validButOldItems = results.b.filter { it.valid }

                assertEquals(4, validButOldItems.size)

                val invalidItems = results.b.filter { !it.valid }
                assertEquals(13, invalidItems.size)

                assertTrue(
                    validButOldItems.all {
                        checkIfFileItemValid.isValid(
                            it, getPreviousFileItem(validButOldItems, it.path.toString()),
                            profile.fileItemValidationConfig
                        )
                    }
                )
                assertTrue(
                    invalidItems.all {
                        !checkIfFileItemValid.isValid(
                            it,
                            getPreviousFileItem(validButOldItems, it.path.toString()),
                            profile.fileItemValidationConfig
                        )
                    }
                )
            }
            is Either.Left -> throw Exception("assert exception")
        }
    }

    @Test
    fun `Include invalid items into keep range if appropriate cleanup policy activated`() {
        createValidArchiveFiles(indexPath, 3)
        createInvalidArchiveFiles(indexPath, 2)
        createValidArchiveFiles(indexPath, 2)
        createInvalidArchiveFiles(indexPath, 1)

        //

        val cleanUpPolicy = CleanUpPolicy(invalidItemsBeyondOfKeepQuantity = true, allInvalidItems = false)
        val profile = profile.copy(keepItemsQuantity = 2, cleanUpPolicy = cleanUpPolicy)

        assertRightResult(useCase.getFileItems(profile)) { results ->
            assertEquals(5, results.size)
        }
    }

    @Test
    fun `Include all invalid items if all-invalid-items cleanup policy is true`() {
        createValidArchiveFiles(indexPath, 3)
        createInvalidArchiveFiles(indexPath, 2)
        createValidArchiveFiles(indexPath, 2)
        createInvalidArchiveFiles(indexPath, 1)

        //

        val cleanUpPolicy = CleanUpPolicy(invalidItemsBeyondOfKeepQuantity = true, allInvalidItems = true)
        val profile = profile.copy(keepItemsQuantity = 2, cleanUpPolicy = cleanUpPolicy)

        assertRightResult(useCase.getFileItems(profile)) { results ->
            assertEquals(6, results.size)
        }
    }

    @Test
    fun `Return MISCONFIGURATION error when keep-items-quantity equals zero`() {
        createValidArchiveFiles(indexPath, 3)
        createInvalidArchiveFiles(indexPath, 2)
        createValidArchiveFiles(indexPath, 2)
        createInvalidArchiveFiles(indexPath, 1)

        //

        val profile = profile.copy(keepItemsQuantity = 0)

        assertErrorResult(useCase.getFileItems(profile), OperationError.MISCONFIGURATION)
    }

    private fun getPreviousFileItem(fileItems: List<FileItem>, fileItemPath: String): Option<FileItem> {
        val itemsBeforeCurrent = fileItems.takeWhile { it.path.toString() != fileItemPath }

        return if (itemsBeforeCurrent.isNotEmpty()) {
            Some(itemsBeforeCurrent.last())

        } else { None }
    }

}
