package ru.lebe.dev.mrjanitor.usecase

import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.ItemValidationConfig
import java.nio.file.Paths

class CheckIfIndexItemValid {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isFileItemValid(fileItem: FileItem, validationConfig: ItemValidationConfig): Boolean {
        log.info("check if file-item valid: '${fileItem.path}'")
        log.debug(fileItem.toString())
        log.info("validation config:")
        log.info(validationConfig.toString())

        var result = false

        val hashFile = Paths.get("${fileItem.path}.md5").toFile()

        if (hashFile.exists()) {
            val hashFromMd5File = hashFile.readText()

            if (fileItem.hash == hashFromMd5File) {
                log.info("- hash check: success")
                result = true

            } else {
                log.info("hash are different:")
                log.info("- file-item hash: '${fileItem.hash}'")
                log.info("- md5-file hash: '$hashFromMd5File'")
            }

        } else {
            log.error("'${fileItem.path}.md5' wasn't found")
        }

        return result
    }
}
