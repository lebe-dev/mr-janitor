package ru.lebe.dev.mrjanitor.usecase

import org.apache.commons.codec.digest.DigestUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.ItemValidationConfig
import ru.lebe.dev.mrjanitor.util.TestUtils.getRandomFileData
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

internal class CheckIfIndexItemValidTest {

    private lateinit var indexPath: Path

    private lateinit var useCase: CheckIfIndexItemValid

    private val sampleFileName = "sample-file"

    private val validationConfig = ItemValidationConfig(
        md5FileCheck = false, zipTest = false, logFileExists = false, qtyAtLeastAsPreviousValid = false
    )

    private val md5ValidationConfig = validationConfig.copy(md5FileCheck = true)

    private lateinit var randomFileContent: String

    @BeforeEach
    fun setUp() {
        randomFileContent = getRandomFileData()

        indexPath = Files.createTempDirectory("")

        useCase = CheckIfIndexItemValid()
    }

    @AfterEach
    fun tearDown() {
        indexPath.toFile().deleteRecursively()
    }

    @Test
    fun `Return true if file-item passes md5-file hash check`() {
        val (sampleFile, hashFile) = createSampleFileWithHashCompanion()

        val fileItem = getFileItem(sampleFile, hashFile.readText())

        assertTrue(useCase.isFileItemValid(fileItem, md5ValidationConfig))
    }

    @Test
    fun `Md5-hash check - return false if hash file wasn't found`() {
        val (sampleFile,hashFile) = createSampleFileWithHashCompanion()
        hashFile.delete()

        val fileItem = getFileItem(sampleFile)

        assertFalse(useCase.isFileItemValid(fileItem, md5ValidationConfig))
    }

    @Test
    fun `Md5-hash check - return false if hash from md5-file doesn't equal with file-item hash`() {
        val (sampleFile,_) = createSampleFileWithHashCompanion()
        val fileItem = getFileItem(sampleFile)
        assertFalse(useCase.isFileItemValid(fileItem, md5ValidationConfig))
    }

    private fun createSampleFileWithHashCompanion(): Pair<File, File> {
        val sampleFile = Paths.get(indexPath.toString(), sampleFileName)
            .apply { toFile().writeText(randomFileContent) }.toFile()

        val sampleFileHash = sampleFile.inputStream().use { DigestUtils.md5Hex(it) }

        val hashFile = Paths.get(indexPath.toString(), "$sampleFileName.md5")
                            .apply { toFile().writeText(sampleFileHash) }.toFile()

        return Pair(sampleFile, hashFile)
    }

    private fun getFileItem(sampleFile: File, hash: String = UUID.randomUUID().toString()) =
        FileItem(
            path = sampleFile.toPath(),
            name = sampleFile.name,
            size = sampleFile.length(),
            hash = hash
        )
}
