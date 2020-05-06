package ru.lebe.dev.mrjanitor.domain

import arrow.core.Either

typealias OperationResult<R> = Either<OperationError, R>

enum class OperationError {
    ERROR,

    /**
     * Invalid configuration
     */
    MISCONFIGURATION
}
