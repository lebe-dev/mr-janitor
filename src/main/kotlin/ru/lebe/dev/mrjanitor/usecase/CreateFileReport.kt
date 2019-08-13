package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Try
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

class CreateFileReport {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    }

    fun create(file: File, success: Boolean, finished: Date): Try<File> = Try<File> {
        log.info("create file report - '${file.name}'")
        log.debug("- success: $success")
        log.debug("- finished: $finished")

        val properties = Properties()
        properties["success"] = success.toString().toLowerCase()
        properties["finished"] = (finished.time / 1000).toString()
        properties["finishedStr"] = SimpleDateFormat(DATE_FORMAT).format(finished)

        file.outputStream().use { properties.store(it, "Janitor Report | Do not modify, please") }

        log.info("report has been saved")

        file
    }
}