package com.example.demowebflux.rest.client

interface DemoClient {
    suspend fun call(msg: String): String
}
