package com.example.demowebflux.errors

import org.springframework.http.HttpStatus

enum class DemoError(val httpStatus: Int, val code: Int, val message: String) {
    UNEXPECTED_400_ERROR(HttpStatus.BAD_REQUEST, 111, "Unexpected Error"),

    MSG_IS_666(HttpStatus.CONFLICT, 666, "Message can't be 666"),

    UNEXPECTED_500_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1111, "Unexpected Error");

    constructor(httpStatus: HttpStatus, code: Int, message: String) : this(httpStatus.value(), code, message)
}
