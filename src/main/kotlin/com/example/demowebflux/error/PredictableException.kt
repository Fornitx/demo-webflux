package com.example.demowebflux.error

@Suppress("unused")
class PredictableException : RuntimeException {
    companion object {
        private const val MSG = "PredictableError"
    }

    constructor(message: String?) : super("$MSG: $message")
    constructor(message: String?, cause: Throwable?) : super("$MSG: $message", cause)
    constructor(cause: Throwable?) : super(MSG, cause)

    val errorCode = ErrorCodes.PREDICTABLE_ERROR
}
