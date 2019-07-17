package ru.lebe.dev.mrjanitor

import arrow.core.Either
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import ru.lebe.dev.mrjanitor.domain.StorageUnit
import ru.lebe.dev.mrjanitor.presenter.CommandLinePresenter
import ru.lebe.dev.mrjanitor.usecase.CheckIfDirectoryItemValid
import ru.lebe.dev.mrjanitor.usecase.CheckIfFileItemValid
import ru.lebe.dev.mrjanitor.usecase.CreateFileIndex
import ru.lebe.dev.mrjanitor.usecase.GetDirectoryItemsForCleanUp
import ru.lebe.dev.mrjanitor.usecase.GetFileItemsForCleanUp
import ru.lebe.dev.mrjanitor.usecase.ReadConfigFromFile
import java.io.File
import java.util.Date

class App {

    companion object {
        const val VERSION = "1.0.0"
        const val CONFIG_FILE = "janitor.conf"

        @JvmStatic
        fun main(args: Array<String>) {
            val readConfigFromFile = ReadConfigFromFile()

            val createFileIndex = CreateFileIndex()

            val checkIfFileItemValid = CheckIfFileItemValid()

            val getFileItemsForCleanUp = GetFileItemsForCleanUp(createFileIndex, checkIfFileItemValid)

            val checkIfDirectoryItemValid = CheckIfDirectoryItemValid(checkIfFileItemValid)
            val getDirectoryItemsForCleanUp = GetDirectoryItemsForCleanUp(createFileIndex, checkIfDirectoryItemValid)

            val commandLinePresenter = CommandLinePresenter()

            DefaultCommand().subcommands(
                ShowItemsForCleanUpCommand(readConfigFromFile, getFileItemsForCleanUp, getDirectoryItemsForCleanUp,
                commandLinePresenter),
                ShowVersionCommand()
            ).main(args)
        }
    }

    private class DefaultCommand: CliktCommand(name = "java -jar janitor.jar") {
        override fun run() {}
    }

    private class ShowItemsForCleanUpCommand(
        private val readConfigFromFile: ReadConfigFromFile,
        private val getFileItemsForCleanUp: GetFileItemsForCleanUp,
        private val getDirectoryItemsForCleanUp: GetDirectoryItemsForCleanUp,
        private val presenter: CommandLinePresenter

    ): CliktCommand(
        name = "dry-run", help = "show what will be cleaned, but don't delete anything"
    ) {
        override fun run() {
            when(val configFile = readConfigFromFile.read(File(CONFIG_FILE))) {
                is Either.Right -> {

                    configFile.b.profiles.forEach { profile ->

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
                is Either.Left -> {
                    presenter.showError("unable to read configuration from file '$CONFIG_FILE', check logs for details")
                }
            }
        }
    }

    private class ShowVersionCommand: CliktCommand(name = "version", help = "show version") {
        override fun run() {
            println(VERSION)
        }
    }

}
