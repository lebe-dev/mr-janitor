package ru.lebe.dev.mrjanitor.presenter

class CommandLinePresenter: AppPresenter {
    override fun showMessage(message: String) = println(message)
    override fun showError(message: String) = println("[!] error - $message")
    override fun showConfigurationLoadError(configFileName: String) =
        showError("unable to read configuration from file '$configFileName', check logs for details")
}
