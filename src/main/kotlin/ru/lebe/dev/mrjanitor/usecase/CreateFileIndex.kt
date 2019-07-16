package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import arrow.core.Failure
import arrow.core.Success
import arrow.core.Try
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.*
import java.io.IOException
import java.nio.file.Path

class CreateFileIndex {
    private val log = LoggerFactory.getLogger(javaClass)

    fun create(path: Path, storageUnit: StorageUnit): Either<OperationResult, PathFileIndex> {
        log.info("create file index for path '$path'")
        log.info("- storage-unit: $storageUnit")

        return if (path.toFile().exists()) {
            when(storageUnit) {
                StorageUnit.DIRECTORY -> createIndexForDirectories(path)
                StorageUnit.FILE -> createIndexForFiles(path)
            }
        } else {
            log.error("path doesn't exist")
            Either.left(OperationResult.ERROR)
        }
    }

    private fun createIndexForDirectories(path: Path): Either<OperationResult, PathFileIndex> =
        when(val directoryItems = getDirectoryItemsFromPath(path)) {
            is Success -> {
                log.info("index has been created")
                log.debug(directoryItems.value.toString())
                Either.right(directoryItems.value)

                Either.right(
                    PathFileIndex(
                        path = path,
                        storageUnit = StorageUnit.DIRECTORY,
                        directoryItems = directoryItems.value,
                        fileItems = listOf()
                    )
                )
            }
            is Failure -> {
                log.error("unable to create file index")
                log.error(directoryItems.exception.message, directoryItems.exception.cause)
                Either.left(OperationResult.ERROR)
            }
        }

    private fun createIndexForFiles(path: Path): Either<OperationResult, PathFileIndex> =
        when(val fileItems = getFileItemsFromPath(path)) {
            is Try.Success -> {
                Either.right(
                    PathFileIndex(
                        path = path,
                        storageUnit = StorageUnit.FILE,
                        directoryItems = listOf(),
                        fileItems = fileItems.value
                    )
                )
            }
            is Try.Failure -> {
                log.error(
                    "unable to get file items from path: ${fileItems.exception.message}",
                    fileItems.exception.cause
                )
                Either.left(OperationResult.ERROR)
            }
        }

    private fun getDirectoryItemsFromPath(path: Path) = Try<List<DirectoryItem>> {
        val results = arrayListOf<DirectoryItem>()

        path.toFile().listFiles()?.filter { it.isDirectory }?.forEach { directory ->

            when(val fileItems = getFileItemsFromPath(directory.absoluteFile.toPath())) {
                is Success -> {
                    val directorySize = fileItems.value.sumBy { it.size.toInt() }.toLong()

                    results += DirectoryItem(
                        name = directory.name,
                        size = directorySize,
                        fileItems = fileItems.value
                    )
                }
                is Failure -> {
                    throw IOException("unable to get file items for path: ${fileItems.exception.message}")
                }
            }

        }

        results
    }

    private fun getFileItemsFromPath(path: Path) = Try<List<FileItem>> {
        val results = arrayListOf<FileItem>()

        path.toFile().listFiles()?.filter { it.isFile }?.forEach { file ->
            results += FileItem(
                name = file.name,
                size = file.length(),
                hash = file.inputStream().use { DigestUtils.md5Hex(it) }
            )
        }

        results
    }
}
