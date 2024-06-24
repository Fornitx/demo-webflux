package com.example.demowebflux.rest.proxy

import com.example.demowebflux.AbstractMockWebServerTest
import com.example.demowebflux.constants.API_V2
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.utils.JwtTestUtils
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.expectBody
import java.util.*

@SpringBootTest
class ProxyErrorMockWebServerTest : AbstractMockWebServerTest() {
    @Nested
    inner class HeaderTest {
        @Test
        @DisplayName(HEADER_X_REQUEST_ID)
        fun noRequestIdHeader() = runTest {
            val responseBody = webTestClient.get()
                .uri(API_V2)
                .header(HttpHeaders.AUTHORIZATION, JwtTestUtils.TOKEN)
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectBody<String>()
                .returnResult()
                .responseBody

            log.info("responseBody: {}", responseBody)
        }

        @Test
        @DisplayName(HttpHeaders.AUTHORIZATION)
        fun noAuthHeader() = runTest {
            val requestId = UUID.randomUUID().toString()
            val responseBody = webTestClient.get()
                .uri(API_V2)
                .header(HEADER_X_REQUEST_ID, requestId)
                .exchange()
                .expectStatus()
                .isUnauthorized
                .expectHeader()
                .valueEquals(HEADER_X_REQUEST_ID, requestId)
                .expectBody<String>()
                .returnResult()
                .responseBody

            log.info("responseBody: {}", responseBody)
        }
    }

//    fun `500 INTERNAL_SERVER_ERROR`(): List<SocketPolicy> = listOf(
//        SocketPolicy.DISCONNECT_AT_START,
//        SocketPolicy.DISCONNECT_AFTER_REQUEST,
//        SocketPolicy.DISCONNECT_AT_END,
//        SocketPolicy.DISCONNECT_DURING_REQUEST_BODY,
//        SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY,
//    )
//
//    @ParameterizedTest
//    @MethodSource
//    fun `500 INTERNAL_SERVER_ERROR`(socketPolicy: SocketPolicy) = mockWebServer(
//        SERVER_PORT, MockResponse().setSocketPolicy(socketPolicy)
//    ) {
//        val requestId = UUID.randomUUID().toString()
//        val rawResponse = client
//            .post()
//            .uri(validPath)
//            .header(AUTHORIZATION, JwtTestUtils.TOKEN)
//            .header(HEADER_X_REQUEST_ID, requestId)
//            .bodyValue(validBody)
//            .exchange()
//            .expectStatus()
//            .is5xxServerError
//            .expectHeader()
//            .contentType(MediaType.APPLICATION_JSON)
//            .expectHeader()
//            .valueEquals(HEADER_X_REQUEST_ID, requestId)
//            .expectBody<String>()
//            .returnResult()
//            .responseBody
//
//        assertLogger(4)
//
//        log.info { "raw response: $rawResponse" }
//
//        val response = objectMapper.readValue<DemoErrorResponse>(rawResponse!!)
//
//        log.info { "response: $response" }
//
//        assertNoMeter(DemoMetrics::httpServerRequest.name)
//        assertMeter(
//            DemoMetrics::error.name, mapOf(
//                METRICS_TAG_CODE to DemoError.UNEXPECTED_5XX_ERROR.code.toString(),
//                METRICS_TAG_STATUS to DemoError.UNEXPECTED_5XX_ERROR.httpStatus.toString(),
//            )
//        )
//    }
//
//    @Test
//    fun `500 CODEC_SIZE`() = mockWebServer(
//        SERVER_PORT, MockResponse().setBody(RandomStringUtils.randomAlphanumeric(5 * 1024 * 1024))
//    ) {
//        val requestId = UUID.randomUUID().toString()
//        val rawResponse = client
//            .post()
//            .uri(validPath)
//            .header(AUTHORIZATION, JwtTestUtils.TOKEN)
//            .header(HEADER_X_REQUEST_ID, requestId)
//            .bodyValue(validBody)
//            .exchange()
//            .expectStatus()
//            .is5xxServerError
//            .expectHeader()
//            .contentType(MediaType.APPLICATION_JSON)
//            .expectHeader()
//            .valueEquals(HEADER_X_REQUEST_ID, requestId)
//            .expectBody<String>()
//            .returnResult()
//            .responseBody
//
//        assertLogger(4)
//
//        log.info { "raw response: $rawResponse" }
//
//        val response = objectMapper.readValue<DemoErrorResponse>(rawResponse!!)
//
//        log.info { "response: $response" }
//
//        assertNoMeter(DemoMetrics::httpServerRequest.name)
//        assertMeter(
//            DemoMetrics::error.name, mapOf(
//                METRICS_TAG_CODE to DemoError.UNEXPECTED_5XX_ERROR.code.toString(),
//                METRICS_TAG_STATUS to DemoError.UNEXPECTED_5XX_ERROR.httpStatus.toString(),
//            )
//        )
//    }
}
