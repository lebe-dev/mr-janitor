package ru.lebe.dev.mrjanitor.domain

data class CleanUpPolicy(
    val invalidItemsBeyondOfKeepQuantity: Boolean,

    val allInvalidItems: Boolean
)
