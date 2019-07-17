package ru.lebe.dev.mrjanitor.util

import java.text.SimpleDateFormat
import java.util.*

object TestUtils {
    fun getRandomFileData() = UUID.randomUUID().toString()

    fun getDateFromString(dateStr: String) = SimpleDateFormat("yyyy-MM-dd").parse(dateStr)
}
