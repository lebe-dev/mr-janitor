package ru.lebe.dev.mrjanitor.usecase

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.lebe.dev.mrjanitor.util.assertRightResult
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date

internal class CreateFileReportTest {

    private val createFileReport = CreateFileReport()

    @Test
    fun `Report file should contains success property`() {
        val reportFile = Files.createTempFile("", "").toFile()

        assertRightResult(createFileReport.create(reportFile, true, Date())) {
            assertTrue(it.readText().contains("success=true"))
        }
    }

    @Test
    fun `Report file should contains finishedStr property`() {
        val reportFile = Files.createTempFile("", "").toFile()

        val finished = Date()

        val expectedTimestamp = SimpleDateFormat("yyyy-MM-dd HH\\:mm\\:ss").format(finished)

        assertRightResult(createFileReport.create(reportFile, true, finished)) {
            assertTrue(it.readText().contains("finishedStr=$expectedTimestamp"))
        }
    }

    @Test
    fun `Report file should contains finished property`() {
        val reportFile = Files.createTempFile("", "").toFile()

        val finished = Date()

        assertRightResult(createFileReport.create(reportFile, true, finished)) {
            assertTrue(it.readText().contains("finished=${finished.time / 1000}"))
        }
    }
}
