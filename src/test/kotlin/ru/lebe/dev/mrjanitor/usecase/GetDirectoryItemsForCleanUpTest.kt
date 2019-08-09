package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.lebe.dev.mrjanitor.domain.CleanAction
import ru.lebe.dev.mrjanitor.domain.CleanUpPolicy
import ru.lebe.dev.mrjanitor.domain.FileItemValidationConfig
import ru.lebe.dev.mrjanitor.domain.OperationError
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.domain.validation.DirectoryItemValidationConfig
import ru.lebe.dev.mrjanitor.util.Defaults
import ru.lebe.dev.mrjanitor.util.SampleDataProvider
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createDirectory
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createFilesWithAbsentHashFile
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createFilesWithInvalidHash
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createValidArchiveFiles
import ru.lebe.dev.mrjanitor.util.assertErrorResult
import ru.lebe.dev.mrjanitor.util.getDateFromString
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.Date

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

    private lateinit var profile: Profile

    @BeforeEach
    fun setUp() {
        indexPath = Files.createTempDirectory("")

        profile = Profile(
            name = "test",
            path = indexPath.toString(),
            storageUnit = StorageUnit.DIRECTORY,
            fileNameFilter = Regex(Defaults.FILENAME_FILTER_PATTERN),
            directoryNameFilter = Regex(Defaults.DIRECTORY_NAME_FILTER_PATTERN),
            keepItemsQuantity = 2,
            fileItemValidationConfig = fileItemValidationConfig,
            directoryItemValidationConfig = directoryItemValidationConfig,
            cleanUpPolicy = CleanUpPolicy(invalidItemsBeyondOfKeepQuantity = false, allInvalidItems = true),
            cleanAction = CleanAction.JUST_NOTIFY
        )

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
        createValidDirectory("zimbambwa", 2)

        createInvalidDirectory("2019-07-09")

        createValidDateDirectory("2019-07-10", 5)

        // INVALID DATE FORMAT
        createValidDirectory("20190711", 2)

        createValidDateDirectory("2019-07-13", 6)

        // Empty directory
        createDirectory(indexPath, getDateFolderName(getDateFromString("2019-07-14"))) {}

        createValidDateDirectory("2019-07-15", 7)

        createInvalidDirectory("2019-07-16")

        // INVALID DATE FORMAT
        createValidDirectory("20190717", 2)

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

    @Test
    fun `Exclude invalid items beyond of keep quantity`() {
        createValidDateDirectory("2019-07-10", 1)
        createInvalidDirectory("2019-07-11")
        createValidDateDirectory("2019-07-12", 3)
        createValidDateDirectory("2019-07-13", 4)
        createInvalidDirectory("2019-07-14")
        createValidDateDirectory("2019-07-15", 5)

        val results = useCase.getItems(
            profile.copy(
                keepItemsQuantity = 3,
                cleanUpPolicy = CleanUpPolicy(invalidItemsBeyondOfKeepQuantity = true, allInvalidItems = false)
            )
        )

        assertTrue(results.isRight())

        when(results) {
            is Either.Right -> {
                val itemsForCleanUp = results.b

                assertEquals(2, itemsForCleanUp.size)
                assertEquals("2019-07-10", itemsForCleanUp.first().name)
                assertEquals("2019-07-11", itemsForCleanUp.last().name)
            }
            is Either.Left -> throw Exception("assert exception")
        }
    }

    @Test
    fun `Return MISCONFIGURATION error when keep-items-quantity equals zero`() {
        createValidArchiveFiles(indexPath, 3)
        SampleDataProvider.createInvalidArchiveFiles(indexPath, 2)
        createValidArchiveFiles(indexPath, 2)
        SampleDataProvider.createInvalidArchiveFiles(indexPath, 1)

        //

        val profile = profile.copy(keepItemsQuantity = 0)

        assertErrorResult(useCase.getItems(profile), OperationError.MISCONFIGURATION)
    }

    private fun createValidDirectory(directoryName: String, filesAmount: Int) {
        createDirectory(indexPath, directoryName) {
            createValidArchiveFiles(it, filesAmount)
        }
    }

    private fun createValidDateDirectory(directoryDate: String, filesAmount: Int) {
        createDirectory(indexPath, getDateFolderName(getDateFromString(directoryDate))) {
            createValidArchiveFiles(it, filesAmount)
        }
    }

    private fun createInvalidDirectory(directoryDate: String) {
        createDirectory(
            indexPath, getDateFolderName(getDateFromString(directoryDate))
        ) { directoryPath ->
            createFilesWithInvalidHash(directoryPath, 1)
            createFilesWithAbsentHashFile(directoryPath, 1)
            createValidArchiveFiles(directoryPath, 1)
        }
    }

    private fun getDateFolderName(date: Date): String = SimpleDateFormat("yyyy-MM-dd").format(date)
}
