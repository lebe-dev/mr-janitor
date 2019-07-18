package ru.lebe.dev.mrjanitor.presenter

interface AppPresenter {
    fun showMessage(message: String)
    fun showError(message: String)
    fun showConfigurationLoadError(configFileName: String)
}
