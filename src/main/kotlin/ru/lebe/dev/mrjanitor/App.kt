package ru.lebe.dev.mrjanitor

import arrow.core.Either
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import ru.lebe.dev.mrjanitor.interactor.CommandLineInteractor
import ru.lebe.dev.mrjanitor.presenter.AppPresenter
import ru.lebe.dev.mrjanitor.presenter.CommandLinePresenter
import ru.lebe.dev.mrjanitor.usecase.CheckIfDirectoryItemValid
import ru.lebe.dev.mrjanitor.usecase.CheckIfFileItemValid
import ru.lebe.dev.mrjanitor.usecase.CleanUpStorageItems
import ru.lebe.dev.mrjanitor.usecase.CreateFileIndex
import ru.lebe.dev.mrjanitor.usecase.GetDirectoryItemsForCleanUp
import ru.lebe.dev.mrjanitor.usecase.GetFileItemsForCleanUp
import ru.lebe.dev.mrjanitor.usecase.ReadConfigFromFile
import java.io.File

class App {

    companion object {
        const val VERSION = "1.0.0"
        const val CONFIG_FILE = "janitor.conf"

        @JvmStatic
        fun main(args: Array<String>) {
            val createFileIndex = CreateFileIndex()

            val checkIfFileItemValid = CheckIfFileItemValid()

            val getDirectoryItemsForCleanUp = GetDirectoryItemsForCleanUp(
                createFileIndex, CheckIfDirectoryItemValid(checkIfFileItemValid)
            )

            val presenter: AppPresenter = CommandLinePresenter()

            val cleanUpStorageItems = CleanUpStorageItems()

            val interactor = getInteractor(
                getFileItemsForCleanUp = GetFileItemsForCleanUp(createFileIndex, checkIfFileItemValid),
                getDirectoryItemsForCleanUp = getDirectoryItemsForCleanUp,
                cleanUpStorageItems = cleanUpStorageItems,
                presenter = presenter
            )

            val readConfigFromFile = ReadConfigFromFile()

            DefaultCommand().subcommands(
                CleanUpCommand(readConfigFromFile, interactor, presenter),
                ShowItemsForCleanUpCommand(readConfigFromFile, interactor, presenter),
                ShowVersionCommand()
            ).main(args)
        }

        private fun getInteractor(getFileItemsForCleanUp: GetFileItemsForCleanUp,
                                  getDirectoryItemsForCleanUp: GetDirectoryItemsForCleanUp,
                                  cleanUpStorageItems: CleanUpStorageItems,
                                  presenter: AppPresenter) =
            CommandLineInteractor(
                getFileItemsForCleanUp = getFileItemsForCleanUp,
                getDirectoryItemsForCleanUp = getDirectoryItemsForCleanUp, presenter = presenter,
                cleanUpStorageItems = cleanUpStorageItems
            )

        private class DefaultCommand: CliktCommand(name = "java -jar janitor.jar") {
            override fun run() {}
        }

        private class CleanUpCommand(
                private val readConfigFromFile: ReadConfigFromFile,
                private val interactor: CommandLineInteractor,
                private val presenter: AppPresenter
        ): CliktCommand(
                name = "cleanup", help = "remove unnecessary directory\\file items"
        ) {
            override fun run() {

                when(val configFile = readConfigFromFile.read(File(CONFIG_FILE))) {
                    is Either.Right -> interactor.cleanup(configFile.b.profiles)
                    is Either.Left -> presenter.showConfigurationLoadError(CONFIG_FILE)
                }
            }
        }

        private class ShowItemsForCleanUpCommand(
            private val readConfigFromFile: ReadConfigFromFile,
            private val interactor: CommandLineInteractor,
            private val presenter: AppPresenter
        ): CliktCommand(
            name = "dry-run", help = "show what will be cleaned, but don't delete anything"
        ) {
            override fun run() {

                when(val configFile = readConfigFromFile.read(File(CONFIG_FILE))) {
                    is Either.Right -> interactor.executeDryRun(configFile.b.profiles)
                    is Either.Left -> presenter.showConfigurationLoadError(CONFIG_FILE)
                }
            }
        }

        private class ShowVersionCommand: CliktCommand(name = "version", help = "show version") {
            override fun run() { println(VERSION) }
        }
    }

}
