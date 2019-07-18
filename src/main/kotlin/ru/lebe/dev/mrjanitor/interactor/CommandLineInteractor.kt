package ru.lebe.dev.mrjanitor.interactor

import arrow.core.Either
import ru.lebe.dev.mrjanitor.domain.DirectoryItem
import ru.lebe.dev.mrjanitor.domain.FileItem
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.presenter.AppPresenter
import ru.lebe.dev.mrjanitor.usecase.GetDirectoryItemsForCleanUp
import ru.lebe.dev.mrjanitor.usecase.GetFileItemsForCleanUp
import java.util.Date

class CommandLineInteractor(
    private val getFileItemsForCleanUp: GetFileItemsForCleanUp,
    private val getDirectoryItemsForCleanUp: GetDirectoryItemsForCleanUp,
    private val presenter: AppPresenter
) {

    fun executeDryRun(profiles: List<Profile>) {
        profiles.forEach { profile ->

            when(profile.storageUnit) {
                StorageUnit.DIRECTORY -> {
                    when(val directoryItems = getDirectoryItemsForCleanUp.getItems(profile)) {
                        is Either.Right -> showDirectoryItemsForCleanUp(directoryItems.b)
                        is Either.Left -> {
                            presenter.showError("unable to get directory items " +
                                    "for cleanup, profile '${profile.name}'")
                        }
                    }
                }
                StorageUnit.FILE -> {

                    when(val fileItems = getFileItemsForCleanUp.getFileItems(profile)) {
                        is Either.Right -> showFileItemsForCleanUp(fileItems.b)
                        is Either.Left -> {
                            presenter.showError("unable to get file items " +
                                    "for cleanup, profile '${profile.name}'")
                        }
                    }

                }
            }
        }
    }

    private fun showDirectoryItemsForCleanUp(directoryItems: List<DirectoryItem>) {
        if (directoryItems.isNotEmpty()) {

            presenter.showMessage("directory items for clean up:")

            directoryItems.forEach { directoryItem ->
                presenter.showMessage("- '${directoryItem.name}'")
                presenter.showMessage("  - valid: ${directoryItem.valid}")
                presenter.showMessage(
                    "  - last-modified: " +
                            "${Date(directoryItem.path.toFile().lastModified())}"
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
