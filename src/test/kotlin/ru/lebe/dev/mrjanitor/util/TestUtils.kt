package ru.lebe.dev.mrjanitor.util

import arrow.core.Either
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import ru.lebe.dev.mrjanitor.domain.OperationError
import ru.lebe.dev.mrjanitor.domain.OperationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

fun <E> assertRightResult(result: OperationResult<E>, body: (E) -> Unit) {
    assertTrue(result.isRight())

    when(result) {
        is Either.Right -> body(result.b)
        is Either.Left -> throw Exception("assert error")
    }
}

fun <E> assertErrorResult(result: OperationResult<E>, operationError: OperationError) {
    assertTrue(result.isLeft())

    when(result) {
        is Either.Left -> assertEquals(operationError, result.a)
        is Either.Right -> throw Exception("assert error")
    }
}

fun getRandomFileData() = UUID.randomUUID().toString()

fun getDateFromString(dateStr: String): Date = SimpleDateFormat("yyyy-MM-dd").parse(dateStr)
