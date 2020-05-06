package ru.lebe.dev.mrjanitor.util

import arrow.core.Either
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.OperationError
import ru.lebe.dev.mrjanitor.domain.OperationResult
import ru.lebe.dev.mrjanitor.util.Defaults.EXIT_CODE_OK
import java.io.BufferedReader
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

object CommandExecutor {
    private val log = LoggerFactory.getLogger(javaClass)

    private const val DEFAULT_PROCESS_WAIT_TIME = 60L

    fun execute(command: String, workDir: File = getWorkDirPath().toFile()): OperationResult<ExecutionResult> =
        try {
            log.debug("execute command:")
            log.debug("command: '$command'")
            log.debug("workdir: '${workDir.absolutePath}'")

            val process = Runtime.getRuntime().exec(command, arrayOf<String>(), workDir)
            process.waitFor()

            val (stdout, stderr) = getProcessStandardStreams(process)

            process.waitFor(DEFAULT_PROCESS_WAIT_TIME, TimeUnit.MINUTES)
            process.destroy()

            log.debug("exit code: '${process.exitValue()}'")

            Either.right(
                ExecutionResult(
                stdout = stdout,
                stderr = stderr,
                exitCode = process.exitValue()
            ))
        } catch (e: Exception) {
            log.error("command execution error '${e.message}'", e)
            Either.left(OperationError.ERROR)
        }

    data class ExecutionResult(
        val stdout: String,
        val stderr: String = "",
        val exitCode: Int = EXIT_CODE_OK
    ) {
        fun isSuccess() = exitCode == EXIT_CODE_OK
    }

    private fun getWorkDirPath(): Path = Paths.get(System.getProperty("user.dir"))

    private fun getProcessStandardStreams(process: Process): Pair<String, String> {
        log.debug("-----[stdout]-----")
        val stdout = process.inputStream.bufferedReader().use { readStreamToString(it) }
        log.debug(stdout)
        log.debug("-----[/stdout]-----")

        log.debug("-----[stderr]-----")
        val stderr = process.errorStream.bufferedReader().use { readStreamToString(it) }
        log.debug(stderr)
        log.debug("-----[/stderr]-----")

        return Pair(stdout, stderr)
    }

    private fun readStreamToString(inputStream: BufferedReader) =
            buildString {
                inputStream.use {
                    var line = it.readLine()

                    while (line != null) {
                        log.debug(line)
                        append(line + System.lineSeparator())
                        line = it.readLine()
                    }
                }
            }

}
