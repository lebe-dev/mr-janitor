package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import arrow.core.Failure
import arrow.core.Success
import arrow.core.Try
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.OperationError
import ru.lebe.dev.mrjanitor.domain.PathFileIndex
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

class CreateFileIndex {
    private val log = LoggerFactory.getLogger(javaClass)

    fun create(profile: Profile): Either<OperationError, PathFileIndex> {
        log.info("create file index for path '${profile.path}'")
        log.info("- storage-unit: ${profile.storageUnit}")

        val profilePath = Paths.get(profile.path)

        return if (profilePath.toFile().exists()) {
            when(profile.storageUnit) {
                StorageUnit.DIRECTORY -> createIndexForDirectories(
                    profilePath, profile.directoryNameFilter,
                    profile.fileNameFilter, profile.fileItemValidationConfig.md5FileCheck
                )
                StorageUnit.FILE ->
                    createIndexForFiles(
                        profilePath, profile.fileNameFilter, profile.fileItemValidationConfig.md5FileCheck
                    )
            }
        } else {
            log.error("path doesn't exist")
            Either.left(OperationError.ERROR)
        }
    }

    private fun createIndexForDirectories(path: Path, directoryNameFilter: Regex,
                              fileNameFilter: Regex, md5HashRequired: Boolean): Either<OperationError, PathFileIndex> =

        when(val directoryItems = getDirectoryItemsFromPath(
                path, directoryNameFilter, fileNameFilter, md5HashRequired)
            ) {
            is Success -> {
                log.info("index has been created")
                log.debug(directoryItems.value.toString())

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
                Either.left(OperationError.ERROR)
            }
        }

    private fun createIndexForFiles(path: Path, fileNameFilter: Regex,
                                    md5HashRequired: Boolean): Either<OperationError, PathFileIndex> =
        when(val fileItems = getFileItemsFromPath(path, fileNameFilter, md5HashRequired)) {
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
                Either.left(OperationError.ERROR)
            }
        }

    private fun getDirectoryItemsFromPath(path: Path, directoryNameFilter: Regex,
                                          fileNameFilter: Regex, md5HashRequired: Boolean) = Try<List<DirectoryItem>> {

    val results = arrayListOf<DirectoryItem>()

        path.toFile().listFiles()?.filter { it.isDirectory && directoryNameFilter.matches(it.name) }
                                 ?.sortedBy { it.name }?.forEach { directory ->

            when(val fileItems = getFileItemsFromPath(directory.toPath(), fileNameFilter, md5HashRequired)) {
                is Success -> {
                    val directorySize = fileItems.value.sumBy { it.size.toInt() }.toLong()

                    results += DirectoryItem(
                        path = directory.toPath(),
                        name = directory.name,
                        size = directorySize,
                        fileItems = fileItems.value,
                        valid = false
                    )
                }
                is Failure -> throw IOException("unable to get file items for path: ${fileItems.exception.message}")
            }

        }

        results
    }

    private fun getFileItemsFromPath(path: Path, fileNameFilter: Regex,
                                     md5HashRequired: Boolean) = Try<List<FileItem>> {
        val results = arrayListOf<FileItem>()

        path.toFile().listFiles()?.filter { it.isFile && fileNameFilter.matches(it.name) }
                                 ?.filterNot { it.extension.toLowerCase() in listOf("md5", "log") }
                                 ?.sortedBy { it.name }
                                 ?.forEach { file ->
            results += FileItem(
                path = file.absoluteFile.toPath(),
                name = file.name,
                size = file.length(),
                hash = getMd5Hash(file, md5HashRequired),
                valid = false
            )
        }

        results
    }

    private fun getMd5Hash(file: File, md5HashRequired: Boolean) =
            if (md5HashRequired) {
                file.inputStream().use { DigestUtils.md5Hex(it) }
            } else { "" }
}
