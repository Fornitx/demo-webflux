package com.example.demowebflux.data

import java.time.Instant

data class DemoResponse(
    val msg: String,
    val instant: Instant = Instant.now()
)
