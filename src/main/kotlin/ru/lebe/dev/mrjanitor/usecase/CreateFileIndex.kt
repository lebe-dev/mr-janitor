package ru.lebe.dev.mrjanitor.usecase

import arrow.core.Either
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.OperationError
import ru.lebe.dev.mrjanitor.domain.OperationResult
import ru.lebe.dev.mrjanitor.domain.PathFileIndex
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class CreateFileIndex {
    private val log = LoggerFactory.getLogger(javaClass)

    fun create(profile: Profile): OperationResult<PathFileIndex> {
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
                              fileNameFilter: Regex, md5HashRequired: Boolean): OperationResult<PathFileIndex> =

        when(val directoryItems = getDirectoryItemsFromPath(
                path, directoryNameFilter, fileNameFilter, md5HashRequired)
            ) {
            is Either.Right -> {
                log.info("index has been created")
                log.debug(directoryItems.b.toString())

                Either.right(
                    PathFileIndex(
                        path = path,
                        storageUnit = StorageUnit.DIRECTORY,
                        directoryItems = directoryItems.b,
                        fileItems = listOf()
                    )
                )
            }
            is Either.Left -> {
                log.error("unable to create file index for path '$path'")
                Either.left(OperationError.ERROR)
            }
        }

    private fun createIndexForFiles(path: Path, fileNameFilter: Regex,
                                    md5HashRequired: Boolean): OperationResult<PathFileIndex> =
        when(val fileItems = getFileItemsFromPath(path, fileNameFilter, md5HashRequired)) {
            is Either.Right -> {
                Either.right(
                    PathFileIndex(
                        path = path,
                        storageUnit = StorageUnit.FILE,
                        directoryItems = listOf(),
                        fileItems = fileItems.b
                    )
                )
            }
            is Either.Left -> {
                log.error("unable to get file items from path: $path")
                Either.left(OperationError.ERROR)
            }
        }

    private fun getDirectoryItemsFromPath(path: Path, directoryNameFilter: Regex,
                                          fileNameFilter: Regex,
                                          md5HashRequired: Boolean): OperationResult<List<DirectoryItem>> =

        try {
            log.debug("get directory items from path '$path', directory-name-filter '$directoryNameFilter'")
            log.debug("file-name-filter '$fileNameFilter', md5-hash-required: $md5HashRequired")

            var hasErrors = false

            val results = arrayListOf<DirectoryItem>()

            path.toFile().listFiles()?.filter { it.isDirectory && directoryNameFilter.matches(it.name) }
                                     ?.sortedBy { it.name }?.forEach { directory ->

                if (!hasErrors) {
                    when(val fileItems =
                                        getFileItemsFromPath(directory.toPath(), fileNameFilter, md5HashRequired)) {
                        is Either.Right -> {
                            log.debug("file-items obtained: ${fileItems.b.size}")

                            var directorySize = 0L

                            fileItems.b.forEach { directorySize += it.size }

                            log.debug("- directory size: $directorySize")

                            val directoryItem = DirectoryItem(
                                path = directory.toPath(),
                                name = directory.name,
                                size = directorySize,
                                fileItems = fileItems.b,
                                valid = false
                            )

                            log.debug("- file items: ${fileItems.b.size}")

                            results += directoryItem
                        }
                        is Either.Left -> hasErrors = true
                    }
                }
            }

            log.debug("directory items obtained: ${results.size}")

            if (!hasErrors) {
                Either.right(results)

            } else {
                Either.left(OperationError.ERROR)
            }

        } catch (e: Exception) {
            log.error("unable to get directory items from path '$path': ${e.message}", e)
            Either.left(OperationError.ERROR)
        }

    private fun getFileItemsFromPath(path: Path, fileNameFilter: Regex,
                                     md5HashRequired: Boolean): OperationResult<List<FileItem>> =
        try {
            log.debug("get file items from path '$path', file-name-filter '$fileNameFilter', " +
                "md5-hash-required: $md5HashRequired")

            val results = arrayListOf<FileItem>()

            path.toFile().listFiles()?.filter { it.isFile && fileNameFilter.matches(it.name) }
                ?.filterNot { it.extension.toLowerCase() in listOf("md5", "log") }
                ?.sortedBy { it.name }
                ?.forEach { file ->

                    val fileItem = FileItem(
                        path = file.absoluteFile.toPath(),
                        name = file.name,
                        size = file.length(),
                        hash = getMd5Hash(file, md5HashRequired),
                        valid = false
                    )

                    log.debug(fileItem.toString())

                    results += fileItem
                }

            log.debug("- items: ${results.size}")

            Either.right(results)

        } catch (e: Exception) {
            log.error("unable to get files items from path '$path': ${e.message}", e)
            Either.left(OperationError.ERROR)
        }

    private fun getMd5Hash(file: File, md5HashRequired: Boolean) =
            if (md5HashRequired) {
                file.inputStream().use { DigestUtils.md5Hex(it) }
            } else { "" }
}
