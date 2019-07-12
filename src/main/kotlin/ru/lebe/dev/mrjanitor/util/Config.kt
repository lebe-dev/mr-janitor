package ru.lebe.dev.mrjanitor.util

import com.typesafe.config.Config

fun Config.getString(path: String, defaultValue: String) =
    if (hasPath(path)) {
        getString(path).orEmpty()
    } else {
        defaultValue
    }

fun Config.getInt(path: String, defaultValue: Int) =
    if (hasPath(path)) {
        getInt(path)
    } else {
        defaultValue
    }
