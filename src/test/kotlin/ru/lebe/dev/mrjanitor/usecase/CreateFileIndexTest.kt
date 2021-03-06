package ru.lebe.dev.mrjanitor.usecase

import org.apache.commons.codec.digest.DigestUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.lebe.dev.mrjanitor.domain.CleanAction
import ru.lebe.dev.mrjanitor.domain.CleanUpPolicy
import ru.lebe.dev.mrjanitor.domain.FileItemValidationConfig
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.domain.validation.DirectoryItemValidationConfig
import ru.lebe.dev.mrjanitor.util.Defaults
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createDirectory
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.getSampleArchiveFileWithCompanions
import ru.lebe.dev.mrjanitor.util.assertErrorResult
import ru.lebe.dev.mrjanitor.util.assertRightResult
import ru.lebe.dev.mrjanitor.util.getRandomText
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

internal class CreateFileIndexTest {

    private lateinit var useCase: CreateFileIndex

    private lateinit var indexPath: Path

    private val directoryValidationConfig = DirectoryItemValidationConfig(
        sizeAtLeastAsPrevious = true,
        filesQtyAtLeastAsInPrevious = false,
        fileSizeAtLeastAsInPrevious = false
    )

    private val fileItemValidationConfig = FileItemValidationConfig(
        sizeAtLeastAsPrevious = true,
        md5FileCheck = true, zipTest = true, logFileExists = true,
        useCustomValidator = false, customValidatorCommand = ""
    )

    private lateinit var profile: Profile

    @BeforeEach
    fun setUp() {
        indexPath = Files.createTempDirectory("")

        profile = Profile(
            name = "default", path = indexPath.toString(), storageUnit = StorageUnit.DIRECTORY,
            fileNameFilter = Regex(Defaults.FILENAME_FILTER_PATTERN),
            directoryNameFilter = Regex(Defaults.DIRECTORY_NAME_FILTER_PATTERN), keepItemsQuantity = 3,
            directoryItemValidationConfig = directoryValidationConfig,
            fileItemValidationConfig = fileItemValidationConfig,
            cleanUpPolicy = CleanUpPolicy(invalidItemsBeyondOfKeepQuantity = true, allInvalidItems = false),
            cleanAction = CleanAction.JUST_NOTIFY
        )

        useCase = CreateFileIndex()
    }

    @AfterEach
    fun tearDown() {
        indexPath.toFile().deleteRecursively()
    }

    @Test
    fun `Path index with file storage unit should return file items`() {
        val firstFileData = getRandomText()
        val firstFile = Paths.get(indexPath.toString(), "bucks.jpg").toFile().apply { writeText(firstFileData) }
        val firstFileHash = DigestUtils.md5Hex(firstFile.readBytes())

        val secondFileData = getRandomText()
        val secondFile = Paths.get(indexPath.toString(), "bunny.jpg").toFile().apply { writeText(secondFileData) }
        val secondFileHash = DigestUtils.md5Hex(secondFile.readBytes())

        val profile = profile.copy(storageUnit = StorageUnit.FILE)

        assertRightResult(useCase.create(profile)) { results ->
            assertEquals(indexPath, results.path)
            assertEquals(StorageUnit.FILE, results.storageUnit)
            assertEquals(2, results.fileItems.size)
            assertTrue(results.directoryItems.isEmpty())

            val firstItem = results.fileItems.first()
            assertEquals(Paths.get(indexPath.toString(), firstItem.name).toString(), firstItem.path.toString())
            assertEquals(firstFile.name, firstItem.name)
            assertEquals(firstFile.length(), firstItem.size)
            assertEquals(firstFileHash, firstItem.hash)

            val secondItem = results.fileItems.last()
            assertEquals(Paths.get(indexPath.toString(), secondItem.name).toString(), secondItem.path.toString())
            assertEquals(secondItem.name, secondFile.name)
            assertEquals(secondFile.length(), secondItem.size)
            assertEquals(secondFileHash, secondItem.hash)
        }
    }

    @Test
    fun `Path index with directory storage unit should return directory and file items`() {
        val firstSubDirectory = Paths.get(indexPath.toString(), "2019-07-16").apply { toFile().mkdir() }

        val firstFileData = getRandomText()
        val firstFile = Paths.get(firstSubDirectory.toString(), getRandomText())
                             .toFile().apply { writeText(firstFileData) }
        val firstFileHash = DigestUtils.md5Hex(firstFile.readBytes())

        val secondSubDirectory = Paths.get(indexPath.toString(), "2019-07-17")
                                      .apply { toFile().mkdir() }
        val secondFileData = getRandomText()
        val secondFile = Paths.get(secondSubDirectory.toString(), getRandomText()).toFile()
                              .apply { writeText(secondFileData) }
        val secondFileHash = DigestUtils.md5Hex(secondFile.readBytes())

        assertRightResult(useCase.create(profile)) { results ->
            assertEquals(StorageUnit.DIRECTORY, results.storageUnit)
            assertEquals(2, results.directoryItems.size)
            assertTrue(results.fileItems.isEmpty())

            val firstDirectory = results.directoryItems.first()
            assertEquals(firstSubDirectory.toFile().name, firstDirectory.name)
            assertEquals(1, firstDirectory.fileItems.size)
            assertEquals(firstFile.length(), firstDirectory.size)

            val firstDirectoryFileItem = firstDirectory.fileItems.first()
            assertEquals(
                Paths.get(firstSubDirectory.toString(), firstFile.name).toString(),
                firstDirectoryFileItem.path.toString()
            )
            assertEquals(firstFile.name, firstDirectoryFileItem.name)
            assertEquals(firstFile.length(), firstDirectoryFileItem.size)
            assertEquals(firstFileHash, firstDirectoryFileItem.hash)

            val secondDirectory = results.directoryItems.last()
            assertEquals(secondSubDirectory.toFile().name, secondDirectory.name)
            assertEquals(1, secondDirectory.fileItems.size)
            assertEquals(secondFile.length(), secondDirectory.size)

            val secondDirectoryFileItem = secondDirectory.fileItems.first()
            assertEquals(
                Paths.get(secondSubDirectory.toString(), secondFile.name).toString(),
                secondDirectoryFileItem.path.toString()
            )
            assertEquals(secondFile.name, secondDirectoryFileItem.name)
            assertEquals(secondFile.length(), secondDirectoryFileItem.size)
            assertEquals(secondFileHash, secondDirectoryFileItem.hash)
        }
    }

    @Test
    fun `Return error if path doesn't exist`() {
        val profile = profile.copy(path = File(getRandomText()).toPath().toString())

        assertErrorResult(useCase.create(profile))
    }

    @Test
    fun `Return empty path-file-index for empty root directory (StorageUnit is File)`() {
        val directory = Files.createTempDirectory("")

        val profile = profile.copy(path = directory.toString(), storageUnit = StorageUnit.FILE)

        assertRightResult(useCase.create(profile)) { results ->
            assertEquals(directory, results.path)
            assertEquals(StorageUnit.FILE, results.storageUnit)
            assertTrue(results.directoryItems.isEmpty())
            assertTrue(results.fileItems.isEmpty())
        }
    }

    @Test
    fun `Return empty path-file-index for empty root directory (StorageUnit is Directory)`() {
        val directory = Files.createTempDirectory("")

        val profile = Profile(
            name = "default", path = directory.toString(),
            storageUnit = StorageUnit.DIRECTORY,
            fileNameFilter = Regex(Defaults.FILENAME_FILTER_PATTERN),
            directoryNameFilter = Regex(Defaults.DIRECTORY_NAME_FILTER_PATTERN), keepItemsQuantity = 3,
            directoryItemValidationConfig = directoryValidationConfig,
            cleanUpPolicy = CleanUpPolicy(invalidItemsBeyondOfKeepQuantity = true, allInvalidItems = false),
            fileItemValidationConfig = fileItemValidationConfig, cleanAction = CleanAction.JUST_NOTIFY
        )

        assertRightResult(useCase.create(profile)) { results ->
            assertEquals(directory, results.path)
            assertEquals(StorageUnit.DIRECTORY, results.storageUnit)
            assertTrue(results.directoryItems.isEmpty())
            assertTrue(results.fileItems.isEmpty())
        }
    }

    @Test
    fun `Files Index - Don't include files excluded by name filter`() {
        val fileNameFilter = ".*\\.zip$"

        val (firstFile, _, _) = getSampleArchiveFileWithCompanions(
            indexPath, UUID.randomUUID().toString()
        )

        Paths.get(indexPath.toString(), getRandomText()).toFile().apply { writeText(getRandomText()) }
        Paths.get(indexPath.toString(), getRandomText()).toFile().apply { writeText(getRandomText()) }

        val profile = profile.copy(storageUnit = StorageUnit.FILE, fileNameFilter = Regex(fileNameFilter))

        assertRightResult(useCase.create(profile)) { results ->
            assertEquals(indexPath, results.path)
            assertEquals(StorageUnit.FILE, results.storageUnit)
            assertEquals(1, results.fileItems.size)
            assertTrue(results.directoryItems.isEmpty())

            val firstItem = results.fileItems.first()
            assertEquals(Paths.get(indexPath.toString(), firstFile.name).toString(), firstItem.path.toString())
        }
    }

    @Test
    fun `Directory Index - Don't include files excluded by name filter`() {
        val fileNameFilter = ".*\\.zip$"

        createDirectoryWithSampleFiles("2019-07-14")

        createDirectory(indexPath, "2019-07-15") { directory ->
            getSampleArchiveFileWithCompanions(directory, UUID.randomUUID().toString())
            getSampleArchiveFileWithCompanions(directory, UUID.randomUUID().toString())

            Paths.get(directory.toString(), getRandomText()).toFile().apply { writeText(getRandomText()) }
        }

        val profile = profile.copy(fileNameFilter = Regex(fileNameFilter))

        assertRightResult(useCase.create(profile)) { results ->
            assertEquals(2, results.directoryItems.size)

            val firstDirectoryItem = results.directoryItems.first()
            assertEquals(1, firstDirectoryItem.fileItems.size)

            val secondDirectoryItem = results.directoryItems.last()
            assertEquals(2, secondDirectoryItem.fileItems.size)
        }
    }

    @Test
    fun `Directory item - Hash property should be blank if md5 check disabled for file validation`() {
        createDirectoryWithSampleFiles("2019-07-14")

        val profile = profile.copy(fileItemValidationConfig = fileItemValidationConfig.copy(md5FileCheck = false))

        assertRightResult(useCase.create(profile)) { results ->
            assertTrue(results.directoryItems.isNotEmpty())
            results.directoryItems.forEach { directoryItem ->
                assertTrue(directoryItem.fileItems.isNotEmpty())
                assertTrue(directoryItem.fileItems.all { it.hash.isBlank() })
            }
        }
    }

    @Test
    fun `File item - Hash property should be blank if md5 check disabled for file validation`() {
        Paths.get(indexPath.toString(), getRandomText()).toFile().apply { writeText(getRandomText()) }
        Paths.get(indexPath.toString(), getRandomText()).toFile().apply { writeText(getRandomText()) }

        val profile = profile.copy(
            storageUnit = StorageUnit.FILE,
            fileItemValidationConfig = fileItemValidationConfig.copy(md5FileCheck = false)
        )

        assertRightResult(useCase.create(profile)) { results ->
            assertTrue(results.fileItems.isNotEmpty())
            assertTrue(results.fileItems.all { it.hash.isBlank() })
        }
    }

    private fun createDirectoryWithSampleFiles(directoryName: String) {
        createDirectory(indexPath, directoryName) { directory ->
            val (_, _, _) = getSampleArchiveFileWithCompanions(
                directory, UUID.randomUUID().toString()
            )

            Paths.get(directory.toString(), getRandomText()).toFile().apply { writeText(getRandomText()) }
            Paths.get(directory.toString(), getRandomText()).toFile().apply { writeText(getRandomText()) }
        }
    }
}
