package com.example.demowebflux.errors

import org.springframework.http.HttpStatus

enum class DemoError(val httpStatus: Int, val code: Int, val message: String) {
    UNEXPECTED_4XX_ERROR(HttpStatus.BAD_REQUEST, 111, "Unexpected 4xx Error"),

    MSG_IS_666(HttpStatus.CONFLICT, 666, "Message can't be 666"),

    UNEXPECTED_5XX_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1111, "Unexpected 5xx Error");

    constructor(httpStatus: HttpStatus, code: Int, message: String) : this(httpStatus.value(), code, message)

    fun toPair() = this.httpStatus to this
}
