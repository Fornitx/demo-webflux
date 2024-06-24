package com.example.demowebflux.actuator

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
@AutoConfigureWebTestClient
class SecondActuatorControllerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun test() {
        webTestClient.get()
            .uri("/actuator/health/liveness")
            .exchange()
            .expectStatus()
            .isOk
        webTestClient.get()
            .uri("/actuator/health/readiness")
            .exchange()
            .expectStatus()
            .isOk
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus()
            .isNotFound
    }
}
