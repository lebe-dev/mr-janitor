package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
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
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal class GetFileItemsForCleanUpTest {

    private lateinit var indexPath: Path

    private val sampleBaseFileName = "sample-archive"

    private val createFileIndex = CreateFileIndex()
    private val checkIfFileItemValid = CheckIfFileItemValid()

    private lateinit var useCase: GetFileItemsForCleanUp

    private val validationConfig = FileItemValidationConfig(
        md5FileCheck = false, zipTest = false, logFileExists = true
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
        val (archiveWithInvalidHash1, _, _) = getSampleArchiveFileWithCompanions(
            indexPath, "archive1", hash = "invalid-hash"
        )

        getSampleArchiveFileWithCompanions(indexPath, "archive2")

        val (archiveWithoutHash1, archiveHashFile3, _) = getSampleArchiveFileWithCompanions(
            indexPath, "archive3"
        )
        archiveHashFile3.delete()

        val (_, archiveHashFile4, _) = getSampleArchiveFileWithCompanions(
            indexPath, "archive4"
        )
        archiveHashFile4.delete()

        val (_, _, logFile5) = getSampleArchiveFileWithCompanions(indexPath, "archive5")
        logFile5.delete()

        val invalidArchiveFile = File(javaClass.getResource("/invalid-archive.zip").toURI())
        val file6 = Paths.get(indexPath.toString(), "archive6.zip").toFile()
        invalidArchiveFile.copyTo(file6, true)
        createMd5CompanionFile(indexPath, invalidArchiveFile)
        createLogCompanionFile(indexPath, invalidArchiveFile)

        getSampleArchiveFileWithCompanions(indexPath, "archive7")

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
            keepCopies = 2,
            fileItemValidationConfig = fileItemValidationConfig,
            directoryItemValidationConfig = directoryItemValidationConfig,
            cleanAction = CleanAction.JUST_NOTIFY
        )

        val results = useCase.getFileItems(profile)

        assertTrue(results.isRight())

        when(results) {
            is Either.Right -> {
                assertEquals(5, results.b.size)

                val firstFile = results.b.first()

                assertEquals(archiveWithInvalidHash1.name, firstFile.name)
                assertEquals(archiveWithInvalidHash1.length(), firstFile.size)
                assertFalse(firstFile.valid)

                val secondFile = results.b[1]
                assertEquals(archiveWithoutHash1.name, secondFile.name)
                assertEquals(archiveWithoutHash1.length(), secondFile.size)
                assertFalse(secondFile.valid)

                listOf(1,3,4,5,6).forEach { archiveFileIndex ->
                    assertNotNull(results.b.find { it.name == "archive$archiveFileIndex.zip" })
                }
            }
            is Either.Left -> throw Exception("assert exception")
        }
    }

}
