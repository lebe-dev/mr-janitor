package ru.lebe.dev.mrjanitor.interactor

import arrow.core.Either
import arrow.core.Try
import org.slf4j.LoggerFactory
import ru.lebe.dev.mrjanitor.domain.CleanAction
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.presenter.AppPresenter
import ru.lebe.dev.mrjanitor.usecase.CleanUpStorageItems
import ru.lebe.dev.mrjanitor.usecase.CreateFileReport
import ru.lebe.dev.mrjanitor.usecase.GetDirectoryItemsForCleanUp
import ru.lebe.dev.mrjanitor.usecase.GetFileItemsForCleanUp
import ru.lebe.dev.mrjanitor.util.Defaults
import ru.lebe.dev.mrjanitor.util.Defaults.LOG_ROW_BOLD_SEPARATOR
import java.io.File
import java.util.Date

class CommandLineInteractor(
    private val getFileItemsForCleanUp: GetFileItemsForCleanUp,
    private val getDirectoryItemsForCleanUp: GetDirectoryItemsForCleanUp,
    private val cleanUpStorageItems: CleanUpStorageItems,
    private val createFileReport: CreateFileReport,
    private val presenter: AppPresenter
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun cleanup(profiles: List<Profile>) {
        profiles.forEach { profile ->
            showProfileBanner(profile)
            presenter.showMessage("~ getting items for clean up..")

            when(profile.storageUnit) {
                StorageUnit.DIRECTORY -> {
                    presenter.showMessage("cleanup items for profile '${profile.name}'")
                    cleanUpDirectoryItems(profile)
                    presenter.showMessage("---")
                    presenter.showMessage("completed")
                }
                StorageUnit.FILE -> getFileItemsForCleanUp(profile) { fileItems ->
                    when(profile.cleanAction) {
                        CleanAction.REMOVE -> {
                            presenter.showMessage("cleanup items: ${fileItems.size}")
                            presenter.showMessage("cleanup..")
                            cleanUpFileItems(fileItems)
                            presenter.showMessage("---")
                            presenter.showMessage("completed")
                        }
                        else -> showFileItemsForCleanUp(fileItems)
                    }
                }
            }
        }
    }

    fun executeDryRun(profiles: List<Profile>) {
        profiles.forEach { profile ->
            showProfileBanner(profile)
            presenter.showMessage("~ getting items for clean up..")

            when(profile.storageUnit) {
                StorageUnit.DIRECTORY -> getDirectoryItemsForCleanUp(profile) { showDirectoryItemsForCleanUp(it) }
                StorageUnit.FILE -> getFileItemsForCleanUp(profile) { showFileItemsForCleanUp(it) }
            }
        }
    }

    private fun getDirectoryItemsForCleanUp(profile: Profile, body: (List<DirectoryItem>) -> Unit) {
        when(val directoryItems = getDirectoryItemsForCleanUp.getItems(profile)) {
            is Either.Right -> body(directoryItems.b)
            is Either.Left -> presenter.showError("unable to get directory items " +
                                                  "for cleanup, profile '${profile.name}'")
        }
    }

    private fun getFileItemsForCleanUp(profile: Profile, body: (List<FileItem>) -> Unit) {
        when(val fileItems = getFileItemsForCleanUp.getItems(profile)) {
            is Either.Right -> body(fileItems.b)
            is Either.Left -> presenter.showError("unable to get file items " +
                                                  "for cleanup, profile '${profile.name}'")
        }
    }

    private fun cleanUpFileItems(fileItems: List<FileItem>) {
        when(cleanUpStorageItems.cleanUp(fileItems)) {
            is Try.Success -> createSuccessFileReport()
            is Try.Failure -> createFailureFileReport()
        }
    }

    private fun showProfileBanner(profile: Profile) {
        presenter.showMessage(LOG_ROW_BOLD_SEPARATOR)
        presenter.showMessage(" PROFILE '${profile.name}'")
        presenter.showMessage(LOG_ROW_BOLD_SEPARATOR)
        presenter.showMessage("- path: '${profile.path}'")
        presenter.showMessage("- storage-unit: ${profile.storageUnit.toString().toLowerCase()}")
        presenter.showMessage("- keep copies: ${profile.keepItemsQuantity}")
    }

    private fun cleanUpDirectoryItems(profile: Profile) {
        getDirectoryItemsForCleanUp(profile) { directoryItems ->
            when(profile.cleanAction) {
                CleanAction.REMOVE -> {
                    when(cleanUpStorageItems.cleanUp(directoryItems)) {
                        is Try.Success -> createSuccessFileReport()
                        is Try.Failure -> createFailureFileReport()
                    }
                }
                else -> showDirectoryItemsForCleanUp(directoryItems)
            }
        }
    }

    private fun createSuccessFileReport() = createFileReport(true)
    private fun createFailureFileReport() = createFileReport(false)

    private fun createFileReport(success: Boolean) =
                                    when(createFileReport.create(File(Defaults.REPORT_FILE_NAME), success, Date())) {
                                        is Try.Success -> log.debug("file report has been saved")
                                        is Try.Failure -> log.error("unable to create file report")
                                    }

    private fun showDirectoryItemsForCleanUp(directoryItems: List<DirectoryItem>) {
        if (directoryItems.isNotEmpty()) {

            presenter.showMessage("directory items for clean up (${directoryItems.size}):")

            directoryItems.forEach { directoryItem ->
                presenter.showMessage("- '${directoryItem.name}'")
                presenter.showMessage("  - files: ${directoryItem.fileItems.size}")
                presenter.showMessage("  - valid: ${directoryItem.valid}")
                presenter.showMessage(
                    "  - last-modified: ${Date(directoryItem.path.toFile().lastModified())}"
                )
            }

            presenter.showMessage("---")
            presenter.showMessage("items total: ${directoryItems.size}")

        } else { presenter.showMessage("nothing to clean up ;)") }
    }

    private fun showFileItemsForCleanUp(fileItems: List<FileItem>) {
        if (fileItems.isNotEmpty()) {

            fileItems.forEach { fileItem ->
                presenter.showMessage("- '${fileItem.name}'")
                presenter.showMessage("  - valid: ${fileItem.valid}")
                presenter.showMessage(
                    "  - last-modified: ${Date(fileItem.path.toFile().lastModified())}"
                )
            }

            presenter.showMessage("---")
            presenter.showMessage("items total: ${fileItems.size}")

        } else { presenter.showMessage("nothing to clean up ;)") }
    }
}
