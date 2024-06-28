//package com.example.demowebflux.openapi
//
//import com.example.demowebflux.AbstractWebTestClientTest
//import org.junit.jupiter.api.Test
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.test.web.reactive.server.expectBody
//
//@SpringBootTest
//class OpenApiTest : AbstractWebTestClientTest() {
//    @Test
//    fun test() {
//        val rawResponse = webTestClient.get()
//            .uri("/v3/api-docs.yaml")
//            .exchange()
//            .expectStatus()
//            .isOk
//            .expectBody<String>()
//            .returnResult()
//            .responseBody
//
//        log.info { "Raw response: \n$rawResponse" }
//    }
//}
