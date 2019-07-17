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
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createLogCompanionFile
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createMd5CompanionFile
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.getSampleArchiveFileWithCompanions
import ru.lebe.dev.mrjanitor.util.TestUtils.getRandomFileData
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

internal class GetFileItemsForCleanUpTest {

    private lateinit var indexPath: Path

    private val createFileIndex = CreateFileIndex()
    private val checkIfFileItemValid = CheckIfFileItemValid()

    private lateinit var useCase: GetFileItemsForCleanUp

    private val invalidArchiveFile = File(javaClass.getResource("/invalid-archive.zip").toURI())

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
        createValidArchiveFiles(2)

        createFilesWithInvalidHash(1)
        createFilesWithAbsentHashFile(1)

        createValidArchiveFiles(2) // GOOD

        createInvalidArchiveFiles(2)

        createValidArchiveFiles(2) // GOOD

        createFilesWithAbsentHashFile(3)
        createFilesWithAbsentLogFile(2)

        createValidArchiveFiles(1) // GOOD

        createInvalidArchiveFiles(2)
        createFilesWithInvalidHash(2)

        createValidArchiveFiles(2) // GOOD

        val fileItemValidationConfig = FileItemValidationConfig(
            md5FileCheck = true, zipTest = true, logFileExists = true
        )

        val directoryItemValidationConfig = DirectoryItemValidationConfig(
            qtyAtLeastAsInPreviousItem = true
        )

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

    private fun createFilesWithAbsentHashFile(amount: Int): List<File> {
        val results = arrayListOf<File>()

        for (index in 1..amount) {
            val (file, hashFile, _) = getSampleArchiveFileWithCompanions(indexPath, getRandomTimeBasedFileName())
            hashFile.delete()
            results += file
        }

        return results
    }

    private fun createFilesWithInvalidHash(amount: Int): List<File> {
        val results = arrayListOf<File>()

        for (index in 1..amount) {
            val (file, hashFile, _) = getSampleArchiveFileWithCompanions(indexPath, getRandomTimeBasedFileName())
            hashFile.writeText(getRandomFileData())
            results += file
        }

        return results
    }

    private fun createFilesWithAbsentLogFile(amount: Int): List<File> {
        val results = arrayListOf<File>()

        for (index in 1..amount) {
            val (file, _, logFile) = getSampleArchiveFileWithCompanions(indexPath, getRandomTimeBasedFileName())
            logFile.delete()
            results += file
        }

        return results
    }

    private fun createValidArchiveFiles(amount: Int): List<File> {
        val results = arrayListOf<File>()

        for (index in 1..amount) {
            val (file, _, _) = getSampleArchiveFileWithCompanions(indexPath, getRandomTimeBasedFileName())
            results += file
        }

        return results
    }

    private fun createInvalidArchiveFiles(amount: Int): List<File> {
        val results = arrayListOf<File>()

        for (index in 1..amount) {
            val file = Paths.get(indexPath.toString(), getRandomTimeBasedFileName()).toFile()
            invalidArchiveFile.copyTo(file, true)
            createMd5CompanionFile(indexPath, file)
            createLogCompanionFile(indexPath, file)
            results += file
        }

        return results
    }

    private fun getRandomTimeBasedFileName(): String {
        val microHash = UUID.randomUUID().toString().take(4)

        return "${SimpleDateFormat("HHmmss-SSS").format(Date())}-$microHash.zip"
    }

}
