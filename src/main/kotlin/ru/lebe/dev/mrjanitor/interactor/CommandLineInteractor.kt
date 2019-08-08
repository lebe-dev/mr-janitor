package ru.lebe.dev.mrjanitor.interactor

import arrow.core.Either
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.presenter.AppPresenter
import ru.lebe.dev.mrjanitor.usecase.CleanUpStorageItems
import ru.lebe.dev.mrjanitor.usecase.GetDirectoryItemsForCleanUp
import ru.lebe.dev.mrjanitor.usecase.GetFileItemsForCleanUp
import ru.lebe.dev.mrjanitor.util.Defaults.LOG_ROW_BOLD_SEPARATOR
import java.util.Date

class CommandLineInteractor(
    private val getFileItemsForCleanUp: GetFileItemsForCleanUp,
    private val getDirectoryItemsForCleanUp: GetDirectoryItemsForCleanUp,
    private val cleanUpStorageItems: CleanUpStorageItems,
    private val presenter: AppPresenter
) {

    fun cleanup(profiles: List<Profile>) {
        profiles.forEach { profile ->
            showProfileBanner(profile)
            presenter.showMessage("~ getting items for clean up..")

            when(profile.storageUnit) {
                StorageUnit.DIRECTORY -> cleanUpDirectoryItems(profile)
                StorageUnit.FILE -> {
                    when(val fileItems = getFileItemsForCleanUp.getFileItems(profile)) {
                        is Either.Right -> cleanUpStorageItems.cleanUp(fileItems.b)
                        is Either.Left -> presenter.showError("unable to get file items " +
                                                              "for cleanup, profile '${profile.name}'")
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
                StorageUnit.DIRECTORY -> {
                    when(val directoryItems = getDirectoryItemsForCleanUp.getItems(profile)) {
                        is Either.Right -> showDirectoryItemsForCleanUp(directoryItems.b)
                        is Either.Left -> presenter.showError("unable to get directory items " +
                                                              "for cleanup, profile '${profile.name}'")
                    }
                }
                StorageUnit.FILE -> {

                    when(val fileItems = getFileItemsForCleanUp.getFileItems(profile)) {
                        is Either.Right -> showFileItemsForCleanUp(fileItems.b)
                        is Either.Left -> presenter.showError("unable to get file items " +
                                                              "for cleanup, profile '${profile.name}'")
                    }

                }
            }
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
        when(val directoryItems = getDirectoryItemsForCleanUp.getItems(profile)) {
            is Either.Right -> cleanUpStorageItems.cleanUp(directoryItems.b)
            is Either.Left -> presenter.showError("unable to get directory items " +
                    "for cleanup, profile '${profile.name}'")
        }
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

        } else {
            presenter.showMessage("nothing to clean up ;)")
        }
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

        } else {
            presenter.showMessage("nothing to clean up ;)")
        }
    }
}
