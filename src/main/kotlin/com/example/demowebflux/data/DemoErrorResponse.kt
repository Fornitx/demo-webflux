package com.example.demowebflux.data

import java.time.OffsetDateTime

data class DemoErrorResponse(
    val timestamp: OffsetDateTime,
    val path: String,
    val requestId: String?,
    val status: Int,
    val code: Int,
    val message: String,
    val detailedMessage: String? = null
)
