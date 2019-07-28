package ru.lebe.dev.mrjanitor.usecase

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.FileItemValidationConfig
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

class CheckIfFileItemValid {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isValid(fileItem: FileItem, previousFileItem: Option<FileItem>,
                validationConfig: FileItemValidationConfig): Boolean {

        log.info("---")
        log.info("check if file-item valid: '${fileItem.path}'..")
        log.debug(fileItem.toString())
        log.debug("validation config:")
        log.debug(validationConfig.toString())

        var result = false

        var nextCheckLock = false

        if (fileItem.path.toFile().exists()) {

            if (validationConfig.md5FileCheck) {
                result = isMd5CheckSuccess(fileItem)

                log.debug("- md5-hash check success: $result")

                if (!result) { nextCheckLock = true }
            }

            if (!nextCheckLock && validationConfig.sizeAtLeastAsPrevious) {
                result = isPreviousFileItemSizeCheckSuccess(fileItem, previousFileItem)

                log.debug("- previous file-item size check success: $result")

                if (!result) { nextCheckLock = true }
            }

            if (!nextCheckLock && validationConfig.logFileExists) {
                result = isLogFileCheckSuccess(fileItem)

                log.debug("- log-file-exists check success: $result")

                if (!result) { nextCheckLock = true }
            }

            if (!nextCheckLock && validationConfig.zipTest) {
                result = isArchiveFileCheckSuccess(fileItem)

                log.debug("- zip-test success: $result")
            }

        } else {
            log.error("file item wasn't found at path '${fileItem.path}'")
        }

        log.info("- item valid: $result")

        return result
    }

    private fun isPreviousFileItemSizeCheckSuccess(currentFileItem: FileItem,
                                                   previousFileItem: Option<FileItem>) =
        when(previousFileItem) {
            is Some -> {
                log.debug("check size difference between current and previous items:")
                log.debug("current: ${currentFileItem.size} vs. previous size: ${previousFileItem.t.size}")
                currentFileItem.size >= previousFileItem.t.size
            }
            is None -> {
                log.debug("there is no previous file-item, check success: true")
                true
            }
        }

    private fun isMd5CheckSuccess(fileItem: FileItem): Boolean {
        var result = false

        val hashFile = Paths.get("${fileItem.path}.md5").toFile()

        if (hashFile.exists()) {
            val hashFromMd5File = hashFile.readText()

            if (fileItem.hash == hashFromMd5File) {
                log.info("- hash check: success")
                result = true

            } else {
                log.info("hashes are different:")
                log.info("- file-item hash: '${fileItem.hash}'")
                log.info("- md5-file hash: '$hashFromMd5File'")
            }

        } else {
            log.error("'${fileItem.path}.md5' wasn't found")
        }

        return result
    }

    private fun isLogFileCheckSuccess(fileItem: FileItem): Boolean {
        var result = false

        log.debug("log-file existence check")

        when(val logFile = findLogFileCompanion(fileItem.path)) {
            is Some -> {
                log.debug("log-file found at path '${logFile.t.toPath()}'")

                if (isLogFileValid(logFile.t)) {
                    log.debug("log-file found at path ''")
                    result = true

                } else {
                    log.debug("log-file wasn't found")
                }
            }
            is None -> log.debug("log-file wasn't found")
        }

        return result
    }

    private fun findLogFileCompanion(filePath: Path): Option<File> {
        val logFilePath1 = Paths.get(
            filePath.parent.toString(), "${filePath.toFile().nameWithoutExtension}.log"
        ).toFile()

        return if (logFilePath1.exists()) {
            Some(logFilePath1)

        } else {
            val logFilePath2 = Paths.get("$filePath.log").toFile()

            if (logFilePath2.exists()) {
                Some(logFilePath2)

            } else {
                None
            }
        }
    }

    private fun isArchiveFileCheckSuccess(fileItem: FileItem): Boolean {
        log.debug("archive file integrity check")

        return if (isArchiveFileValid(fileItem.path.toFile())) {
            log.debug("archive file integrity - ok")
            true

        } else {
            log.debug("archive is corrupted")
            false
        }
    }

    private fun isLogFileValid(file: File) = file.exists() && file.length() > 0

    private fun isArchiveFileValid(file: File): Boolean {
        var result = false

        var zipFile: ZipFile? = null

        try {
            zipFile = ZipFile(file)
            result = true

        } catch (e: IOException) {
            log.error(e.message, e)

        } finally {
            try {
                zipFile?.close()

            } catch (e: IOException) {
                log.error(e.message, e)
            }
        }

        return result
    }
}
