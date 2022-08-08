package com.example.demowebflux.data

data class DemoErrorResponse(val status: Status) {
    constructor(code: Int, message: String) : this(Status(code, message))

    data class Status(val code: Int, val message: String)
}
