package com.example.demowebflux.rest.client

import com.example.demowebflux.properties.DemoProperties
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange

class DemoClientImpl(private val properties: DemoProperties.ClientProperties) : DemoClient {
    override suspend fun call(msg: String): String {
        return WebClient.builder()
            .build()
            .post()
            .uri(properties.url)
            .bodyValue(msg)
            .awaitExchange<ResponseEntity<String>>(ClientResponse::awaitEntity)
            .body!!
    }
}
