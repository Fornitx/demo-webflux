package com.example.demowebflux.rest

import org.springframework.stereotype.Component

@Component
class DemoClient {
    suspend fun call(msg: String): String {
        return msg.repeat(3)
    }
}
