package ru.lebe.dev.mrjanitor.util

import arrow.core.Either
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import ru.lebe.dev.mrjanitor.domain.OperationResult
import java.text.SimpleDateFormat
import java.util.UUID

fun <E> assertRightResult(result: Either<OperationResult, E>, body: (E) -> Unit) {
    assertTrue(result.isRight())

    when(result) {
        is Either.Right -> body(result.b)
        is Either.Left -> throw Exception("assert error")
    }
}

fun <E> assertErrorResult(result: Either<OperationResult, E>, operationResult: OperationResult) {
    assertTrue(result.isLeft())

    when(result) {
        is Either.Left -> assertEquals(operationResult, result.a)
        is Either.Right -> throw Exception("assert error")
    }
}

fun getRandomFileData() = UUID.randomUUID().toString()

fun getDateFromString(dateStr: String) = SimpleDateFormat("yyyy-MM-dd").parse(dateStr)
