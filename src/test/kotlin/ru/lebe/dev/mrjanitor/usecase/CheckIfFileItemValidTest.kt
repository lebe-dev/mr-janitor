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
import java.util.*

internal class CheckIfFileItemValidTest {

    private lateinit var indexPath: Path

    private lateinit var useCase: CheckIfFileItemValid

    private val sampleFileName = "sample-file"

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
        val (sampleFile, hashFile) = createSampleFileWithHashCompanion()

        val fileItem = getFileItem(sampleFile, hashFile.readText())

        assertTrue(useCase.isValid(fileItem, md5ValidationConfig))
    }

    @Test
    fun `Md5-hash check - return false if hash file wasn't found`() {
        val (sampleFile,hashFile) = createSampleFileWithHashCompanion()
        hashFile.delete()

        val fileItem = getFileItem(sampleFile)

        assertFalse(useCase.isValid(fileItem, md5ValidationConfig))
    }

    @Test
    fun `Md5-hash check - return false if hash from md5-file doesn't equal with file-item hash`() {
        val (sampleFile,_) = createSampleFileWithHashCompanion()
        val fileItem = getFileItem(sampleFile)
        assertFalse(useCase.isValid(fileItem, md5ValidationConfig))
    }

    @Test
    fun `Log-file check - return false if log file doesn't exists`() {
        val (sampleFile, logFile) = createSampleFileWithLogCompanion()

        logFile.delete()

        val fileItem = getFileItem(sampleFile)

        assertFalse(useCase.isValid(fileItem, logFileValidationConfig))
    }

    @Test
    fun `Log-file check - return false if log file has zero size`() {
        val (sampleFile, logFile) = createSampleFileWithLogCompanion()

        logFile.delete()
        logFile.createNewFile()

        val fileItem = getFileItem(sampleFile)

        assertFalse(useCase.isValid(fileItem, logFileValidationConfig))
    }

    @Test
    fun `Return false if at least one check has been failure`() {
        val (sampleFile,hashFile) = createSampleFileWithHashCompanion()

        val fileItem = getFileItem(sampleFile, hashFile.readText())

        assertFalse(
            useCase.isValid(
                fileItem,
                validationConfig.copy(
                    md5FileCheck = true, logFileExists = true, zipTest = true
                )
            )
        )
    }

    @Test
    fun `Zip-archive check - return true if zip file is valid`() {
        val archiveFile = getSampleArchiveFile()
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
        val archiveFile = getSampleArchiveFile()

        val md5Hash = archiveFile.inputStream().use { DigestUtils.md5Hex(it) }

        Paths.get("${archiveFile.absoluteFile.toPath()}.md5").apply { toFile().writeText(md5Hash) }
        Paths.get("${archiveFile.absoluteFile.toPath()}.log").apply { toFile().writeText("whatever-log-data") }

        val fileItem = getFileItem(archiveFile, md5Hash)

        assertTrue(useCase.isValid(fileItem, checkAllValidationConfig))
    }

    private fun createSampleFileWithHashCompanion(): Pair<File, File> {
        val sampleFile = Paths.get(indexPath.toString(), sampleFileName)
            .apply { toFile().writeText(randomFileContent) }.toFile()

        val sampleFileHash = sampleFile.inputStream().use { DigestUtils.md5Hex(it) }

        val hashFile = Paths.get(indexPath.toString(), "$sampleFileName.md5")
                            .apply { toFile().writeText(sampleFileHash) }.toFile()

        return Pair(sampleFile, hashFile)
    }

    private fun createSampleFileWithLogCompanion(): Pair<File, File> {
        val sampleFile = Paths.get(indexPath.toString(), sampleFileName)
            .apply { toFile().writeText(randomFileContent) }.toFile()

        val logFile = Paths.get(indexPath.toString(), "$sampleFileName.log")
            .apply { toFile().writeText(randomFileContent) }.toFile()

        return Pair(sampleFile, logFile)
    }

    private fun getSampleArchiveFile(): File {
        val referenceArchiveFile = File(javaClass.getResource("/sample-archive.zip").toURI())

        val resultFile = Paths.get(indexPath.toString(), "sample-archive.zip").toFile()

        referenceArchiveFile.copyTo(resultFile, true)

        return resultFile
    }

    private fun getFileItem(sampleFile: File, hash: String = UUID.randomUUID().toString()) =
        FileItem(
            path = sampleFile.toPath(), name = sampleFile.name,
            size = sampleFile.length(), hash = hash
        )
}
