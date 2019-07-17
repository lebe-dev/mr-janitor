package ru.lebe.dev.mrjanitor.usecase

import org.apache.commons.codec.digest.DigestUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.FileItemValidationConfig
import ru.lebe.dev.mrjanitor.util.TestUtils.getRandomFileData
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

internal class CheckIfFileItemValidTest {

    private lateinit var indexPath: Path

    private lateinit var useCase: CheckIfFileItemValid

    private val sampleFileName = "sample-archive"

    private val validationConfig = FileItemValidationConfig(
        md5FileCheck = false, zipTest = false, logFileExists = false
    )

    private val checkAllValidationConfig = FileItemValidationConfig(
        md5FileCheck = true, zipTest = true,
        logFileExists = true
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
        assertFalse(useCase.isValid(getFileItem(file), checkAllValidationConfig))
    }

    @Test
    fun `Return true if file-item passes md5-file hash check`() {
        val (archiveFile, hashFile, _) = getSampleArchiveFileWithCompanions()

        val fileItem = getFileItem(archiveFile, hashFile.readText())

        assertTrue(useCase.isValid(fileItem, md5ValidationConfig))
    }

    @Test
    fun `Md5-hash check - return false if hash file wasn't found`() {
        val (archiveFile, hashFile, _) = getSampleArchiveFileWithCompanions()
        hashFile.delete()

        val fileItem = getFileItem(archiveFile)

        assertFalse(useCase.isValid(fileItem, md5ValidationConfig))
    }

    @Test
    fun `Md5-hash check - return false if hash from md5-file doesn't equal with file-item hash`() {
        val (archiveFile, _, _) = getSampleArchiveFileWithCompanions()
        val fileItem = getFileItem(archiveFile)
        assertFalse(useCase.isValid(fileItem, md5ValidationConfig))
    }

    @Test
    fun `Log-file check - return false if log file doesn't exists`() {
        val (archiveFile, _, logFile) = getSampleArchiveFileWithCompanions()

        logFile.delete()

        val fileItem = getFileItem(archiveFile)

        assertFalse(useCase.isValid(fileItem, logFileValidationConfig))
    }

    @Test
    fun `Log-file check - return false if log file has zero size`() {
        val (archiveFile, _, logFile) = getSampleArchiveFileWithCompanions()

        logFile.delete()
        logFile.createNewFile()

        val fileItem = getFileItem(archiveFile)

        assertFalse(useCase.isValid(fileItem, logFileValidationConfig))
    }

    @Test
    fun `Return false if at least one check has been failure`() {
        // Without md5 file
        val (archiveFile, hashFile, _) = getSampleArchiveFileWithCompanions()
        hashFile.delete()
        val fileItem = getFileItem(archiveFile)

        assertFalse(useCase.isValid(fileItem, checkAllValidationConfig))

        // Without log file
        val (archiveFile2, hashFile2, logFile2) = getSampleArchiveFileWithCompanions()
        logFile2.delete()
        val fileItem2 = getFileItem(archiveFile2, hashFile2.readText())

        assertFalse(useCase.isValid(fileItem2, checkAllValidationConfig))

        // Log file has zero size
        val (archiveFile3, hashFile3, logFile3) = getSampleArchiveFileWithCompanions()
        logFile3.delete()
        logFile3.createNewFile()
        val fileItem3 = getFileItem(archiveFile3, hashFile3.readText())

        assertFalse(useCase.isValid(fileItem3, checkAllValidationConfig))

        // Invalid archive file
        val invalidArchiveFile = File(javaClass.getResource("/invalid-archive.zip").toURI())
        val hashFile4 = createMd5CompanionFile(indexPath, invalidArchiveFile)
        createLogCompanionFile(indexPath, invalidArchiveFile)

        val fileItem4 = getFileItem(invalidArchiveFile, hashFile4.readText())
        assertFalse(useCase.isValid(fileItem4, checkAllValidationConfig))
    }

    @Test
    fun `Zip-archive check - return true if zip file is valid`() {
        val (archiveFile, _, _) = getSampleArchiveFileWithCompanions()
        val fileItem = getFileItem(archiveFile)

        assertTrue(useCase.isValid(fileItem, validationConfig.copy(zipTest = true)))
    }

    @Test
    fun `Zip-archive check - return false if zip file is not valid`() {
        val invalidArchiveFile = File(javaClass.getResource("/invalid-archive.zip").toURI())

        val fileItem = getFileItem(invalidArchiveFile)

        assertFalse(useCase.isValid(fileItem, validationConfig.copy(zipTest = true)))
    }

    @Test
    fun `Return true if all checks are passed`() {
        val (archiveFile, _, _) = getSampleArchiveFileWithCompanions()

        val md5Hash = archiveFile.inputStream().use { DigestUtils.md5Hex(it) }

        Paths.get("${archiveFile.absoluteFile.toPath()}.md5").apply { toFile().writeText(md5Hash) }
        Paths.get("${archiveFile.absoluteFile.toPath()}.log").apply { toFile().writeText("whatever-log-data") }

        val fileItem = getFileItem(archiveFile, md5Hash)

        assertTrue(useCase.isValid(fileItem, checkAllValidationConfig))
    }

    private fun getSampleArchiveFileWithCompanions(hash: String = ""): Triple<File, File, File> {
        val archiveFileName = "$sampleFileName.zip"

        val referenceArchiveFile = File(javaClass.getResource("/$archiveFileName").toURI())

        val resultFile = Paths.get(indexPath.toString(), archiveFileName).toFile()
        referenceArchiveFile.copyTo(resultFile, true)

        return Triple(
            resultFile, createMd5CompanionFile(indexPath, resultFile, hash),
            createLogCompanionFile(indexPath, resultFile)
        )
    }

    private fun createMd5CompanionFile(path: Path, sourceFile: File, hash: String = ""): File {
        val hashValue = if (hash.isBlank()) {
            sourceFile.inputStream().use { DigestUtils.md5Hex(it) }

        } else {
            getRandomFileData()
        }

        return Paths.get(path.toString(), "${sourceFile.name}.md5").toFile().apply { writeText(hashValue) }
    }

    private fun createLogCompanionFile(path: Path, sourceFile: File): File {
        return Paths.get(path.toString(), "${sourceFile.name}.log").toFile()
                    .apply { writeText(getRandomFileData()) }
    }

    private fun getFileItem(sampleFile: File, hash: String = UUID.randomUUID().toString()) =
        FileItem(
            path = sampleFile.toPath(), name = sampleFile.name,
            size = sampleFile.length(), hash = hash, valid = false
        )
}
