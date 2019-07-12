package ru.lebe.dev.mrjanitor.domain

data class AppConfig(
    val defaultProfile: Profile,

    val profiles: List<Profile>
)
