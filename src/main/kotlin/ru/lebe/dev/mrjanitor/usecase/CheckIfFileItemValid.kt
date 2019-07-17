package ru.lebe.dev.mrjanitor.usecase

import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.FileItemValidationConfig
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.zip.ZipFile

class CheckIfFileItemValid {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isValid(fileItem: FileItem, validationConfig: FileItemValidationConfig): Boolean {
        log.info("check if file-item valid: '${fileItem.path}'")
        log.debug(fileItem.toString())
        log.info("validation config:")
        log.info(validationConfig.toString())

        var result = false

        var nextCheckLock = false

        if (fileItem.path.toFile().exists()) {

            if (validationConfig.md5FileCheck) {
                result = isMd5CheckSuccess(fileItem)

                if (!result) { nextCheckLock = true }
            }

            if (!nextCheckLock && validationConfig.logFileExists) {
                result = isLogFileCheckSuccess(fileItem)

                if (!result) { nextCheckLock = true }
            }

            if (!nextCheckLock && validationConfig.zipTest) {
                result = isArchiveFileCheckSuccess(fileItem)
            }

        } else {
            log.error("file item wasn't found at path '${fileItem.path}'")
        }

        return result
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
        log.debug("log-file existence check")

        val logFile = Paths.get("${fileItem.path}.log").toFile()

        return if (isLogFileValid(logFile)) {
            log.debug("log-file found")
            true

        } else {
            log.debug("log-file wasn't found")
            false
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
