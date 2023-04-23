package com.example.demowebflux.rest.service

import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse

interface DemoService {
    suspend fun foo(request: DemoRequest): DemoResponse
}
