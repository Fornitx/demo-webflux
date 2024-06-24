package com.example.demowebflux.rest.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class WebClientTimeoutException : ResponseStatusException {
    constructor(cause: Throwable) : super(HttpStatus.GATEWAY_TIMEOUT, null, cause)
}
