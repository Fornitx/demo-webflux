package com.example.demowebflux.data

import com.example.demowebflux.errors.DemoError

data class DemoErrorResponse(
    val code: Int,
    val message: String,
    val detailedMessage: String? = null
) {
    constructor(
        demoError: DemoError,
        throwable: Throwable,
    ) : this(
        demoError.code,
        demoError.message,
        throwable.message
    )
}
