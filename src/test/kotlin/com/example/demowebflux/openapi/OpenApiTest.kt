package com.example.demowebflux.openapi

import com.example.demowebflux.AbstractJUnitTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class OpenApiTest : AbstractJUnitTest() {
    @Autowired
    private lateinit var client: WebTestClient

    @Test
    fun test() {
        val rawResponse = client.get()
            .uri("/v3/api-docs.yaml")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<String>()
            .returnResult()
            .responseBody

        log.info { "Raw response: \n$rawResponse" }
    }
}
