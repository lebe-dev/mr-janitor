package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.FileItemValidationConfig
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.domain.validation.DirectoryItemValidationConfig
import ru.lebe.dev.mrjanitor.util.Defaults
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createDirectory
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createFilesWithAbsentHashFile
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createRandomJunkFiles
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createValidArchiveFiles
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

internal class CheckIfDirectoryItemValidTest {

    private lateinit var indexPath: Path

    private val createFileIndex = CreateFileIndex()

    private val checkIfFileItemValid = CheckIfFileItemValid()

    private lateinit var useCase: CheckIfDirectoryItemValid

    private val directoryValidationConfig = DirectoryItemValidationConfig(
        fileSizeAtLeastAsPrevious = true,
        qtyAtLeastAsInPreviousItem = false
    )

    private val fileItemValidationConfig = FileItemValidationConfig(
        fileSizeAtLeastAsPrevious = true,
        md5FileCheck = true, zipTest = true, logFileExists = true
    )

    @BeforeEach
    fun setUp() {
        indexPath = Files.createTempDirectory("")

        useCase = CheckIfDirectoryItemValid(checkIfFileItemValid)
    }

    @AfterEach
    fun tearDown() {
        indexPath.toFile().delete()
    }

    @Test
    fun `Return false if path doesn't exist`() {
        val directoryItem = DirectoryItem(
            path = File("path-does-not-exist").toPath(),
            name = "whatever",
            size = 59235,
            fileItems = listOf(),
            valid = false
        )

        assertFalse(
            useCase.isValid(
                directoryItem, Some(getPreviousItem()), directoryValidationConfig, fileItemValidationConfig
            )
        )
    }

    @Test
    fun `Return false if size equals zero`() {
        val directoryItem = DirectoryItem(
            path = indexPath,
            name = "whatever",
            size = 0,
            fileItems = listOf(),
            valid = false
        )

        assertFalse(
            useCase.isValid(
                directoryItem, Some(getPreviousItem()), directoryValidationConfig, fileItemValidationConfig
            )
        )
    }

    @Test
    fun `Return false if there are no file-items`() {
        val directoryItem = DirectoryItem(
            path = indexPath,
            name = "whatever",
            size = 124124,
            fileItems = listOf(),
            valid = false
        )

        assertFalse(
            useCase.isValid(
                directoryItem, Some(getPreviousItem()), directoryValidationConfig, fileItemValidationConfig
            )
        )
    }

    @Test
    fun `Return false if file-items count is lesser than in previous item`() {
        val fileItem1 = FileItem(
            path = Paths.get("."), name = "whehehee", size = 123, hash = UUID.randomUUID().toString(),
            valid = false
        )

        val fileItem2 = FileItem(
            path = Paths.get("."), name = "bugaga", size = 73457, hash = UUID.randomUUID().toString(),
            valid = false
        )

        val directoryItem = DirectoryItem(
            path = indexPath,
            name = "whatever",
            size = 124124,
            fileItems = listOf(fileItem1),
            valid = false
        )

        assertFalse(
            useCase.isValid(
                directoryItem, Some(getPreviousItem(fileItems = listOf(fileItem2, fileItem1))),
                directoryValidationConfig.copy(qtyAtLeastAsInPreviousItem = true),
                fileItemValidationConfig
            )
        )
    }

    @Test
    fun `Return true if no special validation methods were selected`() {
        val directoryName = "2019-07-15"

        var dirTotalSize = 0

        val directory = createDirectory(indexPath, directoryName) { directoryPath ->
            val files = createValidArchiveFiles(directoryPath, 3)
            val files2 = createFilesWithAbsentHashFile(directoryPath, 1)

            dirTotalSize += files.sumBy { it.length().toInt() }
            dirTotalSize += files2.sumBy { it.length().toInt() }
        }

        val pathIndex = createFileIndex.create(
            indexPath, StorageUnit.DIRECTORY, Regex(Defaults.FILENAME_FILTER_PATTERN)
        )

        assertTrue(pathIndex.isRight())

        when(pathIndex) {
            is Either.Right -> {
                val directoryItem = DirectoryItem(
                    path = directory.toPath(),
                    name = directoryName,
                    size = dirTotalSize.toLong(),
                    fileItems = pathIndex.b.directoryItems.last().fileItems,
                    valid = true
                )

                assertFalse(
                    useCase.isValid(
                        directoryItem, None,
                        directoryValidationConfig.copy(qtyAtLeastAsInPreviousItem = false),
                        fileItemValidationConfig
                    )
                )
            }
            is Either.Left -> throw Exception("assert error")
        }
    }

    @Test
    fun `Return true if all validation methods were passed`() {
        val directoryName1 = "2019-07-13"

        var dirTotalSize1 = 0

        val directory1 = createDirectory(indexPath, directoryName1) { directoryPath ->
            createRandomJunkFiles(directoryPath, 2)

            val files = createValidArchiveFiles(directoryPath, 3)

            dirTotalSize1 = files.sumBy { it.length().toInt() }
        }

        val directoryName2 = "2019-07-14"

        var dirTotalSize2 = 0

        val directory2 = createDirectory(indexPath, directoryName2) { directoryPath ->
            createRandomJunkFiles(directoryPath, 3)

            val files = createValidArchiveFiles(directoryPath, 5)
            dirTotalSize2 = files.sumBy { it.length().toInt() }
        }

        val pathIndex = createFileIndex.create(
            indexPath, StorageUnit.DIRECTORY, Regex(".*\\.zip$")
        )

        assertTrue(pathIndex.isRight())

        when(pathIndex) {
            is Either.Right -> {

                val directoryItem = DirectoryItem(
                    path = directory2.toPath(),
                    name = directoryName2,
                    size = dirTotalSize2.toLong(),
                    fileItems = pathIndex.b.directoryItems.last().fileItems,
                    valid = true
                )

                val previousDirectoryItem = DirectoryItem(
                    path = directory1.toPath(),
                    name = directoryName1,
                    size = dirTotalSize1.toLong(),
                    fileItems = pathIndex.b.directoryItems.first().fileItems,
                    valid = true
                )

                assertTrue(
                    useCase.isValid(
                        directoryItem, Some(previousDirectoryItem),
                        directoryValidationConfig.copy(qtyAtLeastAsInPreviousItem = true),
                        fileItemValidationConfig
                    )
                )

            }
            is Either.Left -> throw Exception("assert error")
        }
    }

    @Test
    fun `Return false if has at least one invalid file item`() {
        val directoryName = "2019-07-15"

        var dirTotalSize = 0

        val directory = createDirectory(indexPath, directoryName) { directoryPath ->
            val files = createValidArchiveFiles(directoryPath, 3)
            val files2 = createFilesWithAbsentHashFile(directoryPath, 1)

            dirTotalSize += files.sumBy { it.length().toInt() }
            dirTotalSize += files2.sumBy { it.length().toInt() }
        }

        val pathIndex = createFileIndex.create(
            indexPath, StorageUnit.DIRECTORY, Regex(Defaults.FILENAME_FILTER_PATTERN)
        )

        assertTrue(pathIndex.isRight())

        when(pathIndex) {
            is Either.Right -> {
                val directoryItem = DirectoryItem(
                    path = directory.toPath(),
                    name = directoryName,
                    size = dirTotalSize.toLong(),
                    fileItems = pathIndex.b.directoryItems.last().fileItems,
                    valid = true
                )

                assertFalse(
                    useCase.isValid(
                        directoryItem, None,
                        directoryValidationConfig.copy(qtyAtLeastAsInPreviousItem = true),
                        fileItemValidationConfig
                    )
                )
            }
            is Either.Left -> throw Exception("assert error")
        }
    }

    private fun getPreviousItem(path: Path = indexPath, size: Long = 12345L,
                                fileItems: List<FileItem> = listOf()) = DirectoryItem(
        path = path,
        name = "whatever",
        size = size,
        fileItems = fileItems,
        valid = false
    )
}
