package ru.lebe.dev.mrjanitor.interactor

import arrow.core.Either
import ru.lebe.dev.mrjanitor.domain.Profile
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.presenter.AppPresenter
import ru.lebe.dev.mrjanitor.usecase.GetDirectoryItemsForCleanUp
import ru.lebe.dev.mrjanitor.usecase.GetFileItemsForCleanUp
import java.util.*

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
                        is Either.Right -> {

                            if (directoryItems.b.isNotEmpty()) {

                                presenter.showMessage("directory items for clean up:")

                                directoryItems.b.forEach { directoryItem ->
                                    presenter.showMessage("- '${directoryItem.name}'")
                                    presenter.showMessage("  - valid: ${directoryItem.valid}")
                                    presenter.showMessage(
                                        "  - last-modified: " +
                                                "${Date(directoryItem.path.toFile().lastModified())}"
                                    )
                                }

                                presenter.showMessage("---")
                                presenter.showMessage("items total: ${directoryItems.b.size}")

                            } else {
                                presenter.showMessage("nothing to clean up ;)")
                            }

                        }
                        is Either.Left -> {
                            presenter.showError("unable to get directory items " +
                                    "for cleanup, profile '${profile.name}'")
                        }
                    }
                }
                StorageUnit.FILE -> {

                    when(val fileItems = getFileItemsForCleanUp.getFileItems(profile)) {
                        is Either.Right -> {

                            if (fileItems.b.isNotEmpty()) {

                                fileItems.b.forEach { fileItem ->
                                    presenter.showMessage("- '${fileItem.name}'")
                                    presenter.showMessage("  - valid: ${fileItem.valid}")
                                    presenter.showMessage(
                                        "  - last-modified: ${Date(fileItem.path.toFile().lastModified())}"
                                    )
                                }

                                presenter.showMessage("---")
                                presenter.showMessage("items total: ${fileItems.b.size}")

                            } else {
                                presenter.showMessage("nothing to clean up ;)")
                            }

                        }
                        is Either.Left -> {
                            presenter.showError("unable to get file items " +
                                    "for cleanup, profile '${profile.name}'")
                        }
                    }

                }
            }
        }
    }
}
