package ru.lebe.dev.mrjanitor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class App {

    private class DefaultCommand: CliktCommand(name = "java -jar janitor.jar") {
        override fun run() {}
    }

    private class ShowVersionCommand: CliktCommand(name = "version", help = "show version") {
        override fun run() {
            println(VERSION)
        }
    }

    companion object {
        const val VERSION = "1.0.0"

        @JvmStatic
        fun main(args: Array<String>) {
            DefaultCommand().subcommands(ShowVersionCommand()).main(args)
        }
    }

}
