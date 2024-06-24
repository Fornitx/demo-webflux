package com.example.demowebflux.rest.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class BadRequestException : ResponseStatusException {
    constructor(reason: String) : super(HttpStatus.BAD_REQUEST, reason)
    constructor(reason: String, cause: Throwable) : super(HttpStatus.BAD_REQUEST, reason, cause)
}
