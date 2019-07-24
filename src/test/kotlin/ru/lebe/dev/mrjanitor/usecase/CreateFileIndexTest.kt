package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import org.apache.commons.codec.digest.DigestUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.lebe.dev.mrjanitor.domain.OperationResult
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.util.Defaults
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createDirectory
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.getSampleArchiveFileWithCompanions
import ru.lebe.dev.mrjanitor.util.TestUtils.getRandomFileData
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

internal class CreateFileIndexTest {

    private lateinit var useCase: CreateFileIndex

    private lateinit var indexPath: Path

    @BeforeEach
    fun setUp() {
        indexPath = Files.createTempDirectory("")

        useCase = CreateFileIndex()
    }

    @AfterEach
    fun tearDown() {
        indexPath.toFile().deleteRecursively()
    }

    @Test
    fun `Path index with file storage unit should return file items`() {
        val firstFileData = getRandomFileData()
        val firstFile = Paths.get(indexPath.toString(), "bucks.jpg").toFile().apply { writeText(firstFileData) }
        val firstFileHash = DigestUtils.md5Hex(firstFile.readBytes())

        val secondFileData = getRandomFileData()
        val secondFile = Paths.get(indexPath.toString(), "bunny.jpg").toFile().apply { writeText(secondFileData) }
        val secondFileHash = DigestUtils.md5Hex(secondFile.readBytes())

        val results = useCase.create(
            indexPath, StorageUnit.FILE,
            Regex(Defaults.DIRECTORY_NAME_FILTER_PATTERN), Regex(Defaults.FILENAME_FILTER_PATTERN)
        )

        assertTrue(results.isRight())

        when(results) {
            is Either.Right -> {
                assertEquals(indexPath, results.b.path)
                assertEquals(StorageUnit.FILE, results.b.storageUnit)
                assertEquals(2, results.b.fileItems.size)
                assertTrue(results.b.directoryItems.isEmpty())

                val firstItem = results.b.fileItems.first()
                assertEquals(Paths.get(indexPath.toString(), firstItem.name).toString(), firstItem.path.toString())
                assertEquals(firstFile.name, firstItem.name)
                assertEquals(firstFile.length(), firstItem.size)
                assertEquals(firstFileHash, firstItem.hash)

                val secondItem = results.b.fileItems.last()
                assertEquals(Paths.get(indexPath.toString(), secondItem.name).toString(), secondItem.path.toString())
                assertEquals(secondItem.name, secondFile.name)
                assertEquals(secondFile.length(), secondItem.size)
                assertEquals(secondFileHash, secondItem.hash)
            }
            is Either.Left -> throw Exception("assertion error")
        }
    }

    @Test
    fun `Path index with directory storage unit should return directory and file items`() {
        val firstSubDirectory = Paths.get(indexPath.toString(), "2019-07-16").apply { toFile().mkdir() }

        val firstFileData = getRandomFileData()
        val firstFile = Paths.get(firstSubDirectory.toString(), "richard.jpg")
                             .toFile().apply { writeText(firstFileData) }
        val firstFileHash = DigestUtils.md5Hex(firstFile.readBytes())

        val secondSubDirectory = Paths.get(indexPath.toString(), "2019-07-17")
                                      .apply { toFile().mkdir() }
        val secondFileData = getRandomFileData()
        val secondFile = Paths.get(secondSubDirectory.toString(), "bernard.jpg").toFile()
                              .apply { writeText(secondFileData) }
        val secondFileHash = DigestUtils.md5Hex(secondFile.readBytes())

        val results = useCase.create(
            indexPath, StorageUnit.DIRECTORY,
            Regex(Defaults.DIRECTORY_NAME_FILTER_PATTERN), Regex(Defaults.FILENAME_FILTER_PATTERN)
        )

        assertTrue(results.isRight())

        when(results) {
            is Either.Right -> {
                assertEquals(StorageUnit.DIRECTORY, results.b.storageUnit)
                assertEquals(2, results.b.directoryItems.size)
                assertTrue(results.b.fileItems.isEmpty())

                val firstDirectory = results.b.directoryItems.first()
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

                val secondDirectory = results.b.directoryItems.last()
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
            is Either.Left -> throw Exception("assertion error")
        }
    }

    @Test
    fun `Return error if path doesn't exist`() {
        val result = useCase.create(
            File("does-not-exist").toPath(), StorageUnit.DIRECTORY,
            Regex(Defaults.DIRECTORY_NAME_FILTER_PATTERN), Regex(Defaults.FILENAME_FILTER_PATTERN)
        )

        assertTrue(result.isLeft())

        when(result) {
            is Either.Left -> assertEquals(OperationResult.ERROR, result.a)
            is Either.Right -> throw Exception("assert error")
        }
    }

    @Test
    fun `Return empty path-file-index for empty root directory (StorageUnit is File)`() {
        val directory = Files.createTempDirectory("")

        val results = useCase.create(
                directory, StorageUnit.FILE,
                Regex(Defaults.DIRECTORY_NAME_FILTER_PATTERN), Regex(Defaults.FILENAME_FILTER_PATTERN)
        )
        assertTrue(results.isRight())

        when(results) {
            is Either.Right -> {
                assertEquals(directory, results.b.path)
                assertEquals(StorageUnit.FILE, results.b.storageUnit)
                assertTrue(results.b.directoryItems.isEmpty())
                assertTrue(results.b.fileItems.isEmpty())
            }
            is Either.Left -> throw Exception("asser exception")
        }
    }

    @Test
    fun `Return empty path-file-index for empty root directory (StorageUnit is Directory)`() {
        val directory = Files.createTempDirectory("")

        val results = useCase.create(
            directory, StorageUnit.DIRECTORY,
            Regex(Defaults.DIRECTORY_NAME_FILTER_PATTERN), Regex(Defaults.FILENAME_FILTER_PATTERN)
        )
        assertTrue(results.isRight())

        when(results) {
            is Either.Right -> {
                assertEquals(directory, results.b.path)
                assertEquals(StorageUnit.DIRECTORY, results.b.storageUnit)
                assertTrue(results.b.directoryItems.isEmpty())
                assertTrue(results.b.fileItems.isEmpty())
            }
            is Either.Left -> throw Exception("asser exception")
        }
    }

    @Test
    fun `Files Index - Don't include files excluded by name filter`() {
        val fileNameFilter = ".*\\.zip$"

        val (firstFile, _, _) = getSampleArchiveFileWithCompanions(
            indexPath, UUID.randomUUID().toString()
        )

        Paths.get(indexPath.toString(), "bunny.jpg").toFile().apply { writeText(getRandomFileData()) }
        Paths.get(indexPath.toString(), "winny.bmp").toFile().apply { writeText(getRandomFileData()) }

        val results = useCase.create(
            indexPath, StorageUnit.FILE, Regex(Defaults.DIRECTORY_NAME_FILTER_PATTERN), Regex(fileNameFilter)
        )

        assertTrue(results.isRight())

        when(results) {
            is Either.Right -> {
                assertEquals(indexPath, results.b.path)
                assertEquals(StorageUnit.FILE, results.b.storageUnit)
                assertEquals(1, results.b.fileItems.size)
                assertTrue(results.b.directoryItems.isEmpty())

                val firstItem = results.b.fileItems.first()
                assertEquals(Paths.get(indexPath.toString(), firstFile.name).toString(), firstItem.path.toString())
            }
            is Either.Left -> throw Exception("assertion error")
        }
    }

    @Test
    fun `Directory Index - Don't include files excluded by name filter`() {
        val fileNameFilter = ".*\\.zip$"

        createDirectory(indexPath, "2019-07-14") { directory ->
            val (_, _, _) = getSampleArchiveFileWithCompanions(
                directory, UUID.randomUUID().toString()
            )

            Paths.get(directory.toString(), "cowboy.jpg").toFile().apply { writeText(getRandomFileData()) }
            Paths.get(directory.toString(), "outlaw.bmp").toFile().apply { writeText(getRandomFileData()) }
        }

        createDirectory(indexPath, "2019-07-15") { directory ->
            getSampleArchiveFileWithCompanions(directory, UUID.randomUUID().toString())
            getSampleArchiveFileWithCompanions(directory, UUID.randomUUID().toString())

            Paths.get(directory.toString(), "dandy.jpg").toFile().apply { writeText(getRandomFileData()) }
        }

        val results = useCase.create(
            indexPath, StorageUnit.DIRECTORY, Regex(Defaults.DIRECTORY_NAME_FILTER_PATTERN), Regex(fileNameFilter)
        )

        assertTrue(results.isRight())

        when(results) {
            is Either.Right -> {
                assertEquals(2, results.b.directoryItems.size)

                val firstDirectoryItem = results.b.directoryItems.first()
                assertEquals(1, firstDirectoryItem.fileItems.size)

                val secondDirectoryItem = results.b.directoryItems.last()
                assertEquals(2, secondDirectoryItem.fileItems.size)
            }
            is Either.Left -> throw Exception("assertion error")
        }
    }
}
