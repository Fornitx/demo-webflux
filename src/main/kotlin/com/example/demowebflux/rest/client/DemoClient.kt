package com.example.demowebflux.rest.client

import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest

interface DemoClient {
    suspend fun foo(msg: String): String
    suspend fun proxy(request: ServerHttpRequest): ResponseEntity<String>
}
