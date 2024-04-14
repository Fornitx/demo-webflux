package com.example.demowebflux.rest.service

import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest

interface DemoService {
    suspend fun foo(request: DemoRequest): ResponseEntity<DemoResponse>

    suspend fun proxy(request: ServerHttpRequest): ResponseEntity<String>
}
