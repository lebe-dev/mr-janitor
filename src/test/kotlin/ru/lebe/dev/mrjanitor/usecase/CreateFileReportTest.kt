package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Try
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date

internal class CreateFileReportTest {

    private val createFileReport = CreateFileReport()

    @Test
    fun `Report file should contains success property`() {
        val reportFile = Files.createTempFile("", "").toFile()

        assertTrySuccess(createFileReport.create(reportFile, true, Date())) {
            assertTrue(it.readText().contains("success=true"))
        }
    }

    @Test
    fun `Report file should contains finishedStr property`() {
        val reportFile = Files.createTempFile("", "").toFile()

        val finished = Date()

        val expectedTimestamp = SimpleDateFormat("yyyy-MM-dd HH\\:mm\\:ss").format(finished)

        assertTrySuccess(createFileReport.create(reportFile, true, finished)) {
            assertTrue(it.readText().contains("finishedStr=$expectedTimestamp"))
        }
    }

    @Test
    fun `Report file should contains finished property`() {
        val reportFile = Files.createTempFile("", "").toFile()

        val finished = Date()

        assertTrySuccess(createFileReport.create(reportFile, true, finished)) {
            assertTrue(it.readText().contains("finished=${finished.time / 1000}"))
        }
    }

    fun <E> assertTrySuccess(result: Try<E>, body: (E) -> Unit) {
        assertTrue(result.isSuccess())

        when(result) {
            is Try.Success -> body(result.value)
            is Try.Failure -> throw Exception("assert error")
        }
    }
}