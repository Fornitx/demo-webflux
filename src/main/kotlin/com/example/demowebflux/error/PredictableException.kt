package com.example.demowebflux.error

import java.lang.RuntimeException

@Suppress("unused")
class PredictableException : RuntimeException {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)

    companion object {
        const val STATUS = 1500
    }
}
