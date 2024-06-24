package com.example.demowebflux.rest.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class WebClientConnectException : ResponseStatusException {
    constructor(cause: Throwable) : super(HttpStatus.BAD_GATEWAY, null, cause)
}
