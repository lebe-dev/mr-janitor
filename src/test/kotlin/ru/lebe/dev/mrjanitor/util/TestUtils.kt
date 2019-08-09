package ru.lebe.dev.mrjanitor.util

import java.text.SimpleDateFormat
import java.util.UUID

fun getRandomFileData() = UUID.randomUUID().toString()

fun getDateFromString(dateStr: String) = SimpleDateFormat("yyyy-MM-dd").parse(dateStr)
