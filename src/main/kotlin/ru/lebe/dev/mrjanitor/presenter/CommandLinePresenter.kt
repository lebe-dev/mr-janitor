package ru.lebe.dev.mrjanitor.presenter

class CommandLinePresenter {
    fun showMessage(message: String) = println(message)
    fun showError(message: String) = println("[!] error - $message")
}
