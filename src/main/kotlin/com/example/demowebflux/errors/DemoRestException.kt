package com.example.demowebflux.errors

class DemoRestException : RuntimeException {
    val demoError: DemoError

    constructor(demoError: DemoError) : super(demoError.message) {
        this.demoError = demoError
    }

    constructor(demoError: DemoError, cause: Throwable) : super(demoError.message, cause) {
        this.demoError = demoError
    }
}
