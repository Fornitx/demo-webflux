package com.example.demowebflux.data

import com.example.demowebflux.error.ErrorCodes

data class DemoErrorResponse(val status: Status) {
    constructor(code: ErrorCodes, message: String?) : this(Status(code.code, message))

    data class Status(val code: Int, val message: String?)
}
