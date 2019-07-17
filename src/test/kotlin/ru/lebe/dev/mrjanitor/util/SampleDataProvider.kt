package ru.lebe.dev.mrjanitor.util

import org.apache.commons.codec.digest.DigestUtils
import ru.lebe.dev.mrjanitor.domain.FileItem
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

object SampleDataProvider {
    fun getSampleArchiveFileWithCompanions(path: Path, fileBaseName: String,
                                           hash: String = ""): Triple<File, File, File> {
        val archiveFileName = "$fileBaseName.zip"

        val referenceArchiveFile = File(javaClass.getResource("/sample-archive.zip").toURI())

        val resultFile = Paths.get(path.toString(), archiveFileName).toFile()
        referenceArchiveFile.copyTo(resultFile, true)

        return Triple(
            resultFile, createMd5CompanionFile(path, resultFile, hash),
            createLogCompanionFile(path, resultFile)
        )
    }

    fun createMd5CompanionFile(path: Path, sourceFile: File, hash: String = ""): File {
        val hashValue = if (hash.isBlank()) {
            sourceFile.inputStream().use { DigestUtils.md5Hex(it) }

        } else {
            TestUtils.getRandomFileData()
        }

        return Paths.get(path.toString(), "${sourceFile.name}.md5").toFile().apply { writeText(hashValue) }
    }

    fun createLogCompanionFile(path: Path, sourceFile: File): File {
        return Paths.get(path.toString(), "${sourceFile.name}.log").toFile()
            .apply { writeText(TestUtils.getRandomFileData()) }
    }

    fun getFileItem(sampleFile: File, hash: String = UUID.randomUUID().toString()) =
        FileItem(
            path = sampleFile.toPath(), name = sampleFile.name,
            size = sampleFile.length(), hash = hash, valid = false
        )
}
