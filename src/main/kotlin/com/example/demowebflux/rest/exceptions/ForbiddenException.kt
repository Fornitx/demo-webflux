package com.example.demowebflux.rest.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class ForbiddenException : ResponseStatusException {
    constructor(reason: String) : super(HttpStatus.FORBIDDEN, reason)
    constructor(reason: String, cause: Throwable) : super(HttpStatus.FORBIDDEN, reason, cause)
}
