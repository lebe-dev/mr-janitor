package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.lebe.dev.mrjanitor.domain.CleanAction
import ru.lebe.dev.mrjanitor.domain.FileItemValidationConfig
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.domain.validation.DirectoryItemValidationConfig
import ru.lebe.dev.mrjanitor.util.Defaults
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

        createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-09"))
        ) { directoryPath ->

            createFilesWithInvalidHash(directoryPath, 1)
            createFilesWithAbsentHashFile(directoryPath, 1)
            createValidArchiveFiles(directoryPath, 2) // GOOD
        }

        createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-10"))
        ) {
            createValidArchiveFiles(it, 5) // GOOD
        }

        createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-11"))
        ) {
            createValidArchiveFiles(it, 6) // GOOD
        }

        createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-12"))
        ) {
            createValidArchiveFiles(it, 5)
        }

        createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-13"))
        ) {
            createValidArchiveFiles(it, 6) // GOOD
        }

        createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-14"))
        ) {}

        createDirectory(
            indexPath, getDateFolderName(getDateFromString("2019-07-15"))
        ) {
            createValidArchiveFiles(it, 7) // GOOD
        }

        val profile = Profile(
            name = "test",
            path = indexPath.toString(),
            storageUnit = StorageUnit.DIRECTORY,
            fileNameFilter = Regex(Defaults.FILENAME_FILTER_PATTERN),
            directoryNameFilter = Regex(Defaults.DIRECTORY_NAME_FILTER_PATTERN),
            keepItemsQuantity = 2,
            fileItemValidationConfig = fileItemValidationConfig,
            directoryItemValidationConfig = directoryItemValidationConfig,
            cleanAction = CleanAction.JUST_NOTIFY
        )

        val results = useCase.getItems(profile)

        assertTrue(results.isRight())

        when(results) {
            is Either.Right -> {
                assertEquals(5, results.b.size)

                val validButOldItems = results.b.filter { it.valid }

                assertEquals(2, validButOldItems.size)

                val invalidItems = results.b.filter { !it.valid }
                assertEquals(3, invalidItems.size)

                listOf("2019-07-13", "2019-07-15").forEach { directoryName ->
                    assertNull(results.b.find { it.name == directoryName })
                }
            }
            is Either.Left -> throw Exception("assert exception")
        }
    }

    @Test
    fun `Respect directory name filter`() {
        createDirectory(
                indexPath, "zimbambwa"
        ) { directoryPath ->
            createValidArchiveFiles(directoryPath, 2)
        }

        createDirectory(
                indexPath, getDateFolderName(getDateFromString("2019-07-09"))
        ) { directoryPath ->
            createFilesWithInvalidHash(directoryPath, 1)
            createFilesWithAbsentHashFile(directoryPath, 1)
            createValidArchiveFiles(directoryPath, 2) // GOOD
        }

        createValidDirectory("2019-07-10", 5)

        // INVALID DATE FORMAT
        createDirectory(
                indexPath, "20190711"
        ) { directoryPath ->
            createValidArchiveFiles(directoryPath, 2)
        }

        createValidDirectory("2019-07-13", 6)

        createDirectory(indexPath, getDateFolderName(getDateFromString("2019-07-14"))) {}

        createValidDirectory("2019-07-15", 7)

        createDirectory(
                indexPath, getDateFolderName(getDateFromString("2019-07-16"))
        ) { directoryPath ->
            createFilesWithInvalidHash(directoryPath, 1)
            createFilesWithAbsentHashFile(directoryPath, 1)
        }

        // INVALID DATE FORMAT
        createDirectory(
                indexPath, "20190717"
        ) { directoryPath ->
            createValidArchiveFiles(directoryPath, 2)
        }

        val profile = Profile(
                name = "test",
                path = indexPath.toString(),
                storageUnit = StorageUnit.DIRECTORY,
                fileNameFilter = Regex(Defaults.FILENAME_FILTER_PATTERN),
                directoryNameFilter = Regex(Defaults.DIRECTORY_NAME_FILTER_PATTERN),
                keepItemsQuantity = 2,
                fileItemValidationConfig = fileItemValidationConfig,
                directoryItemValidationConfig = directoryItemValidationConfig,
                cleanAction = CleanAction.JUST_NOTIFY
        )

        val results = useCase.getItems(profile)

        assertTrue(results.isRight())

        when(results) {
            is Either.Right -> {
                val validButOldItems = results.b.filter { it.valid }

                assertEquals(1, validButOldItems.size)

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

    private fun createValidDirectory(directoryDate: String, filesAmount: Int) {
        createDirectory(indexPath, getDateFolderName(getDateFromString(directoryDate))) {
            createValidArchiveFiles(it, filesAmount)
        }
    }

    private fun getDateFolderName(date: Date): String = SimpleDateFormat("yyyy-MM-dd").format(date)
}
