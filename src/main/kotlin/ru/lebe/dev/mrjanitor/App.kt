package ru.lebe.dev.mrjanitor

import arrow.core.Either
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import ru.lebe.dev.mrjanitor.interactor.CommandLineInteractor
import ru.lebe.dev.mrjanitor.presenter.AppPresenter
import ru.lebe.dev.mrjanitor.presenter.CommandLinePresenter
import ru.lebe.dev.mrjanitor.usecase.CheckIfDirectoryItemValid
import ru.lebe.dev.mrjanitor.usecase.CheckIfFileItemValid
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
            val readConfigFromFile = ReadConfigFromFile()

            val createFileIndex = CreateFileIndex()

            val checkIfFileItemValid = CheckIfFileItemValid()

            val getFileItemsForCleanUp = GetFileItemsForCleanUp(createFileIndex, checkIfFileItemValid)

            val checkIfDirectoryItemValid = CheckIfDirectoryItemValid(checkIfFileItemValid)
            val getDirectoryItemsForCleanUp = GetDirectoryItemsForCleanUp(createFileIndex, checkIfDirectoryItemValid)

            val presenter: AppPresenter = CommandLinePresenter()

            val interactor = getInteractor(
                getFileItemsForCleanUp = getFileItemsForCleanUp,
                getDirectoryItemsForCleanUp = getDirectoryItemsForCleanUp, presenter = presenter
            )

            DefaultCommand().subcommands(
                ShowItemsForCleanUpCommand(readConfigFromFile, interactor, presenter),
                ShowVersionCommand()
            ).main(args)
        }

        private fun getInteractor(getFileItemsForCleanUp: GetFileItemsForCleanUp,
                                  getDirectoryItemsForCleanUp: GetDirectoryItemsForCleanUp,
                                  presenter: AppPresenter) =
            CommandLineInteractor(
                getFileItemsForCleanUp = getFileItemsForCleanUp,
                getDirectoryItemsForCleanUp = getDirectoryItemsForCleanUp, presenter = presenter
            )

        private class DefaultCommand: CliktCommand(name = "java -jar janitor.jar") {
            override fun run() {}
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
                    is Either.Right -> {
                        interactor.executeDryRun(configFile.b.profiles)
                    }
                    is Either.Left -> presenter.showConfigurationLoadError(CONFIG_FILE)
                }
            }
        }

        private class ShowVersionCommand: CliktCommand(name = "version", help = "show version") {
            override fun run() {
                println(VERSION)
            }
        }
    }

}
