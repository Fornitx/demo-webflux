package com.example.demowebflux.error

@Suppress("unused")
class PredictableException : RuntimeException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)

    companion object {
        const val HTTP_STATUS = 1500
    }
}
