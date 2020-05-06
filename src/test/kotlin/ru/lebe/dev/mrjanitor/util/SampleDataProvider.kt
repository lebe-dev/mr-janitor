package ru.lebe.dev.mrjanitor.util

import org.apache.commons.codec.digest.DigestUtils
import ru.lebe.dev.mrjanitor.domain.FileItem
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
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

    fun getSampleFileWithCompanions(path: Path, fileName: String,
                                    hash: String = "", data: String = ""): Triple<File, File, File> {

        val content = if (data.isBlank()) { getRandomText() } else { data }

        val resultFile = Paths.get(path.toString(), fileName).toFile().apply { writeText(content) }

        return Triple(
            resultFile, createMd5CompanionFile(path, resultFile, hash),
            createLogCompanionFile(path, resultFile)
        )
    }

    fun createMd5CompanionFile(path: Path, sourceFile: File, hash: String = ""): File {
        val hashValue = if (hash.isBlank()) {
            sourceFile.inputStream().use { DigestUtils.md5Hex(it) }

        } else {
            getRandomText()
        }

        return Paths.get(path.toString(), "${sourceFile.name}.md5").toFile().apply { writeText(hashValue) }
    }

    fun createLogCompanionFile(path: Path, sourceFile: File) =
        Paths.get(path.toString(), "${sourceFile.name}.log").toFile()
            .apply { writeText(getRandomText()) }

    fun getFileItem(sampleFile: File, hash: String = UUID.randomUUID().toString()) =
        FileItem(
            path = sampleFile.toPath(), name = sampleFile.name,
            size = sampleFile.length(), hash = hash, valid = false
        )

    fun createFilesWithAbsentHashFile(path: Path, amount: Int): List<File> {
        val results = arrayListOf<File>()

        for (index in 1..amount) {
            val (file, hashFile, _) = getSampleArchiveFileWithCompanions(path, getRandomTimeBasedFileName())
            hashFile.delete()
            results += file
        }

        return results
    }

    fun createFilesWithInvalidHash(path: Path, amount: Int): List<File> {
        val results = arrayListOf<File>()

        for (index in 1..amount) {
            val (file, hashFile, _) = getSampleArchiveFileWithCompanions(path, getRandomTimeBasedFileName())
            hashFile.writeText(getRandomText())
            results += file
        }

        return results
    }

    fun createFilesWithAbsentLogFile(path: Path, amount: Int): List<File> {
        val results = arrayListOf<File>()

        for (index in 1..amount) {
            val (file, _, logFile) = getSampleArchiveFileWithCompanions(path, getRandomTimeBasedFileName())
            logFile.delete()
            results += file
        }

        return results
    }

    fun createRandomJunkFiles(path: Path, amount: Int): List<File> {
        val results = arrayListOf<File>()

        val names = listOf("seasons", "Random", "eureka", "abc", "manowar", "Giovanni", "hex-01")
        val extensions = listOf("txt", "png", "jpg", "pdf", "tiff", "avi")

        for (index in 1..amount) {
            val microHash = UUID.randomUUID()

            val randomFileName = "${names.random()}-$microHash.${extensions.random()}"

            val (file, _, _) = getSampleFileWithCompanions(path, randomFileName)
            results += file
        }

        return results
    }

    fun createValidArchiveFiles(path: Path, amount: Int): List<File> {
        val results = arrayListOf<File>()

        for (index in 1..amount) {
            val (file, _, _) = getSampleArchiveFileWithCompanions(path, getRandomTimeBasedFileName())
            results += file
        }

        return results
    }

    fun createInvalidArchiveFiles(path: Path, amount: Int): List<File> {
        val invalidArchiveFile = File(javaClass.getResource("/invalid-archive.zip").toURI())

        val results = arrayListOf<File>()

        for (index in 1..amount) {
            val file = Paths.get(path.toString(), getRandomTimeBasedFileName()).toFile()
            invalidArchiveFile.copyTo(file, true)
            createMd5CompanionFile(path, file)
            createLogCompanionFile(path, file)
            results += file
        }

        return results
    }

    fun createDirectory(path: Path, directoryName: String, body: (Path) -> Unit): File {
        val directory = Paths.get(path.toString(), directoryName).apply { toFile().mkdirs() }
        body(directory)
        return directory.toFile()
    }

    fun getRandomTimeBasedFileName(): String {
        val microHash = UUID.randomUUID().toString().take(4)

        return "${SimpleDateFormat("HHmmss-SSS").format(Date())}-$microHash.zip"
    }
}
