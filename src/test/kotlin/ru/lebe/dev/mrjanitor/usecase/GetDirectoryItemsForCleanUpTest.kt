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
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createDirectory
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createFilesWithAbsentHashFile
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createFilesWithInvalidHash
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createValidArchiveFiles
import ru.lebe.dev.mrjanitor.util.TestUtils.getDateFromString
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

internal class GetDirectoryItemsForCleanUpTest {

    private lateinit var indexPath: Path

    private val createFileIndex = CreateFileIndex()
    private val checkIfFileItemValid = CheckIfFileItemValid()
    private val checkIfDirectoryItemValid = CheckIfDirectoryItemValid(checkIfFileItemValid)

    private lateinit var useCase: GetDirectoryItemsForCleanUp

    private val fileItemValidationConfig = FileItemValidationConfig(
        md5FileCheck = true, zipTest = true, logFileExists = true
    )

    private val directoryItemValidationConfig = DirectoryItemValidationConfig(
        qtyAtLeastAsInPreviousItem = true
    )

    @BeforeEach
    fun setUp() {
        indexPath = Files.createTempDirectory("")

        useCase = GetDirectoryItemsForCleanUp(createFileIndex, checkIfDirectoryItemValid)
    }

    @AfterEach
    fun tearDown() {
        indexPath.toFile().deleteRecursively()
    }

    @Test
    fun `Result list should be equal to keep-copies value`() {
        val getDateFolderName: (date: Date) -> String = {
            SimpleDateFormat("yyyy-MM-dd").format(it)
        }

        val invalidDirectory1 = createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-09"))
        ) { directoryPath ->

            createFilesWithInvalidHash(directoryPath, 1)
            createFilesWithAbsentHashFile(directoryPath, 1)
            createValidArchiveFiles(directoryPath, 2) // GOOD
        }

        val validDirectory1 = createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-10"))
        ) {
            createValidArchiveFiles(it, 5) // GOOD
        }

        val validDirectory2 = createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-11"))
        ) {
            createValidArchiveFiles(it, 6) // GOOD
        }

        val invalidDirectory2 = createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-12"))
        ) {
            createValidArchiveFiles(it, 5)
        }

        val validDirectory3 = createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-13"))
        ) {
            createValidArchiveFiles(it, 6) // GOOD
        }

        val emptyDirectory = createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-14"))
        ) {}

        val validDirectory4 = createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-15"))
        ) {
            createValidArchiveFiles(it, 7) // GOOD
        }

        val profile = Profile(
            name = "test",
            path = indexPath.toString(),
            storageUnit = StorageUnit.DIRECTORY,
            keepCopies = 2,
            fileItemValidationConfig = fileItemValidationConfig,
            directoryItemValidationConfig = directoryItemValidationConfig,
            cleanAction = CleanAction.JUST_NOTIFY
        )

        val results = useCase.getItems(profile)

        assertTrue(results.isRight())

        when(results) {
            is Either.Right -> {
                val validButOldItems = results.b.filter { it.valid }

                assertEquals(2, validButOldItems.size)

                val invalidItems = results.b.filter { !it.valid }
                assertEquals(3, invalidItems.size)

                listOf("2019-07-13", "2019-07-15").all { directoryName ->
                    var findResult = false

                    val directoryFound = results.b.find { it.name == directoryName }

                    if (directoryFound != null) {
                        assertTrue(directoryFound.valid)
                        findResult = true
                    }

                    findResult
                }
            }
            is Either.Left -> throw Exception("assert exception")
        }
    }
}