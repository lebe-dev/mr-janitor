package ru.lebe.dev.mrjanitor.usecase

import arrow.core.None
import arrow.core.Some
import org.apache.commons.codec.digest.DigestUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.FileItemValidationConfig
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createLogCompanionFile
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.createMd5CompanionFile
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.getFileItem
import ru.lebe.dev.mrjanitor.util.SampleDataProvider.getSampleArchiveFileWithCompanions
import ru.lebe.dev.mrjanitor.util.getRandomFileData
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal class CheckIfFileItemValidTest {

    private lateinit var indexPath: Path

    private lateinit var useCase: CheckIfFileItemValid

    private val sampleBaseFileName = "sample-archive"

    private val validationConfig = FileItemValidationConfig(
        sizeAtLeastAsPrevious = false,
        md5FileCheck = false, zipTest = false, logFileExists = false,
        useCustomValidator = false, customValidatorCommand = ""
    )

    private val checkAllValidationConfig = FileItemValidationConfig(
        sizeAtLeastAsPrevious = true,
        md5FileCheck = true, zipTest = true,
        logFileExists = true,
        useCustomValidator = false, customValidatorCommand = ""
    )

    private val md5ValidationConfig = validationConfig.copy(md5FileCheck = true)
    private val logFileValidationConfig = validationConfig.copy(logFileExists = true)

    private lateinit var randomFileContent: String

    @BeforeEach
    fun setUp() {
        randomFileContent = getRandomFileData()

        indexPath = Files.createTempDirectory("")

        useCase = CheckIfFileItemValid()
    }

    @AfterEach
    fun tearDown() {
        indexPath.toFile().deleteRecursively()
    }

    @Test
    fun `Return false if file-item doesn't exist`() {
        val file = File("file-does-not-exist")
        assertFalse(useCase.isValid(getFileItem(file), None, checkAllValidationConfig))
    }

    @Test
    fun `Return true if file-item passes md5-file hash check`() {
        val (archiveFile, hashFile, _) = getSampleArchiveFileWithCompanions(indexPath, sampleBaseFileName)

        val fileItem = getFileItem(archiveFile, hashFile.readText())

        assertTrue(useCase.isValid(fileItem, None, md5ValidationConfig))
    }

    @Test
    fun `Md5-hash check - return false if hash file wasn't found`() {
        val (archiveFile, hashFile, _) = getSampleArchiveFileWithCompanions(indexPath, sampleBaseFileName)
        hashFile.delete()

        val fileItem = getFileItem(archiveFile)

        assertFalse(useCase.isValid(fileItem, None, md5ValidationConfig))
    }

    @Test
    fun `Md5-hash check - return false if hash from md5-file doesn't equal with file-item hash`() {
        val (archiveFile, _, _) = getSampleArchiveFileWithCompanions(indexPath, sampleBaseFileName)
        val fileItem = getFileItem(archiveFile)
        assertFalse(useCase.isValid(fileItem, None, md5ValidationConfig))
    }

    @Test
    fun `Log-file check - return false if log file doesn't exists`() {
        val (archiveFile, _, logFile) = getSampleArchiveFileWithCompanions(indexPath, sampleBaseFileName)

        logFile.delete()

        val fileItem = getFileItem(archiveFile)

        assertFalse(useCase.isValid(fileItem, None, logFileValidationConfig))
    }

    @Test
    fun `Log-file check - return false if log file has zero size`() {
        val (archiveFile, _, logFile) = getSampleArchiveFileWithCompanions(indexPath, sampleBaseFileName)

        logFile.delete()
        logFile.createNewFile()

        val fileItem = getFileItem(archiveFile)

        assertFalse(useCase.isValid(fileItem, None, logFileValidationConfig))
    }

    @Test
    fun `Return false if at least one check has been failure`() {
        // Without md5 file
        val (archiveFile, hashFile, _) = getSampleArchiveFileWithCompanions(indexPath, sampleBaseFileName)
        hashFile.delete()
        val fileItem = getFileItem(archiveFile)

        assertFalse(useCase.isValid(fileItem, None, checkAllValidationConfig))

        // Without log file
        val (archiveFile2, hashFile2, logFile2) = getSampleArchiveFileWithCompanions(indexPath, sampleBaseFileName)
        logFile2.delete()
        val fileItem2 = getFileItem(archiveFile2, hashFile2.readText())

        assertFalse(useCase.isValid(fileItem2, None, checkAllValidationConfig))

        // Log file has zero size
        val (archiveFile3, hashFile3, logFile3) = getSampleArchiveFileWithCompanions(indexPath, sampleBaseFileName)
        logFile3.delete()
        logFile3.createNewFile()
        val fileItem3 = getFileItem(archiveFile3, hashFile3.readText())

        assertFalse(useCase.isValid(fileItem3, None, checkAllValidationConfig))

        // Invalid archive file
        val invalidArchiveFile = File(javaClass.getResource("/invalid-archive.zip").toURI())
        val hashFile4 = createMd5CompanionFile(indexPath, invalidArchiveFile)
        createLogCompanionFile(indexPath, invalidArchiveFile)

        val fileItem4 = getFileItem(invalidArchiveFile, hashFile4.readText())
        assertFalse(useCase.isValid(fileItem4, None, checkAllValidationConfig))
    }

    @Test
    fun `Zip-archive check - return true if zip file is valid`() {
        val (archiveFile, _, _) = getSampleArchiveFileWithCompanions(indexPath, sampleBaseFileName)
        val fileItem = getFileItem(archiveFile)

        assertTrue(useCase.isValid(fileItem, None, validationConfig.copy(zipTest = true)))
    }

    @Test
    fun `Zip-archive check - return false if zip file is not valid`() {
        val invalidArchiveFile = File(javaClass.getResource("/invalid-archive.zip").toURI())

        val fileItem = getFileItem(invalidArchiveFile)

        assertFalse(useCase.isValid(fileItem, None, validationConfig.copy(zipTest = true)))
    }

    @Test
    fun `Log file check should return true if log file was found in one of two ways`() {
        // APPROACH 1: source-file.zip.log

        val (archiveFile, _, logFile) = getSampleArchiveFileWithCompanions(indexPath, sampleBaseFileName)
        val validationConfig = FileItemValidationConfig(
            sizeAtLeastAsPrevious = true,
            md5FileCheck = false, zipTest = false, logFileExists = true,
            useCustomValidator = false, customValidatorCommand = ""
        )

        val fileItem = FileItem(
            path = archiveFile.toPath(),
            name = archiveFile.name,
            size = archiveFile.length(),
            hash = "somehash",
            valid = true
        )

        assertTrue(useCase.isValid(fileItem, None, validationConfig))

        // APPROACH 2: source-file.log
        logFile.delete()

        Paths.get(archiveFile.parent, "${archiveFile.nameWithoutExtension}.log").toFile()
             .apply { writeText(getRandomFileData()) }

        assertTrue(useCase.isValid(fileItem, None, validationConfig))
    }

    @Test
    fun `Return false if file-item has smaller size than previous one`() {
        val validationConfig = FileItemValidationConfig(
            sizeAtLeastAsPrevious = true, md5FileCheck = false,
            zipTest = false, logFileExists = false,
            useCustomValidator = false, customValidatorCommand = ""
        )

        val firstFile = Paths.get(indexPath.toString(), "file1.txt")
                             .toFile().apply { writeText("some-data") }
        val secondFile = Paths.get(indexPath.toString(), "file2.txt")
                              .toFile().apply { writeText("some-d") }

        val previousFileItem = FileItem(
            path = firstFile.toPath(),
            name = firstFile.name,
            size = firstFile.length(),
            hash = "somehash",
            valid = true
        )

        val currentFileItem = FileItem(
            path = secondFile.toPath(),
            name = secondFile.name,
            size = secondFile.length(),
            hash = "somehash2",
            valid = true
        )

        assertFalse(useCase.isValid(currentFileItem, Some(previousFileItem), validationConfig))
    }

    @Test
    fun `Return true if all checks are passed`() {
        val (previousArchiveFile, previousHashFile, _) = getSampleArchiveFileWithCompanions(
            indexPath, "${sampleBaseFileName}2"
        )
        val (archiveFile, _, _) = getSampleArchiveFileWithCompanions(indexPath, sampleBaseFileName)

        val md5Hash = archiveFile.inputStream().use { DigestUtils.md5Hex(it) }

        Paths.get("${archiveFile.absoluteFile.toPath()}.md5").apply { toFile().writeText(md5Hash) }
        Paths.get("${archiveFile.absoluteFile.toPath()}.log")
             .apply { toFile().writeText(getRandomFileData()) }

        val previousFileItem = getFileItem(previousArchiveFile, previousHashFile.readText())
        val fileItem = getFileItem(archiveFile, md5Hash)

        assertTrue(useCase.isValid(fileItem, Some(previousFileItem), checkAllValidationConfig))
    }

    @Test
    fun `Return false for invalid custom check command`() {
        val fileItemValidationConfig = checkAllValidationConfig.copy(
            sizeAtLeastAsPrevious = false, logFileExists = false, md5FileCheck = false,
            useCustomValidator = true, customValidatorCommand = "invalid-custom-command"
        )

        val (archiveFile, _, _) = getSampleArchiveFileWithCompanions(indexPath, sampleBaseFileName)

        val fileItem = getFileItem(archiveFile)

        assertFalse(useCase.isValid(fileItem, None, fileItemValidationConfig))
    }

    @Test
    fun `Return true if custom check command succeed`() {
        val fileItemValidationConfig = checkAllValidationConfig.copy(
                sizeAtLeastAsPrevious = false, logFileExists = false, md5FileCheck = false,
                useCustomValidator = true, customValidatorCommand = "hostname"
        )

        val (archiveFile, _, _) = getSampleArchiveFileWithCompanions(indexPath, sampleBaseFileName)

        val fileItem = getFileItem(archiveFile)

        assertTrue(useCase.isValid(fileItem, None, fileItemValidationConfig))
    }
}
