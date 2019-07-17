package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.lebe.dev.mrjanitor.domain.CleanAction
import ru.lebe.dev.mrjanitor.domain.FileItemValidationConfig
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.domain.validation.DirectoryItemValidationConfig
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createFilesWithAbsentHashFile
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createFilesWithAbsentLogFile
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createFilesWithInvalidHash
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createInvalidArchiveFiles
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createValidArchiveFiles
import java.nio.file.Files
import java.nio.file.Path

internal class GetFileItemsForCleanUpTest {

    private lateinit var indexPath: Path

    private val createFileIndex = CreateFileIndex()
    private val checkIfFileItemValid = CheckIfFileItemValid()

    private lateinit var useCase: GetFileItemsForCleanUp

    private val fileItemValidationConfig = FileItemValidationConfig(
        md5FileCheck = true, zipTest = true, logFileExists = true
    )

    private val directoryItemValidationConfig = DirectoryItemValidationConfig(
        qtyAtLeastAsInPreviousItem = true
    )

    @BeforeEach
    fun setUp() {
        indexPath = Files.createTempDirectory("")

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

        val profile = Profile(
            name = "test",
            path = indexPath.toString(),
            storageUnit = StorageUnit.FILE,
            keepCopies = 5,
            fileItemValidationConfig = fileItemValidationConfig,
            directoryItemValidationConfig = directoryItemValidationConfig,
            cleanAction = CleanAction.JUST_NOTIFY
        )

        val results = useCase.getFileItems(profile)

        assertTrue(results.isRight())

        when(results) {
            is Either.Right -> {
                val validButOldItems = results.b.filter { it.valid }

                assertEquals(4, validButOldItems.size)

                val invalidItems = results.b.filter { !it.valid }
                assertEquals(13, invalidItems.size)

                assertTrue(validButOldItems.all { checkIfFileItemValid.isValid(it, profile.fileItemValidationConfig) })
                assertTrue(invalidItems.all { !checkIfFileItemValid.isValid(it, profile.fileItemValidationConfig) })
            }
            is Either.Left -> throw Exception("assert exception")
        }
    }

}
