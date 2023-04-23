package com.example.demowebflux.rest.client

import org.springframework.stereotype.Component

@Component
class DemoClientImpl : DemoClient {
    override suspend fun call(msg: String): String {
        return msg.uppercase()
    }
}
