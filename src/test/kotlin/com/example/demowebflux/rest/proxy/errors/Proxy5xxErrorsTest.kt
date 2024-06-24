package com.example.demowebflux.rest.proxy.errors

import com.example.demowebflux.AbstractMockWebServerTest
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.metrics.METRICS_TAG_HTTP_METHOD
import com.example.demowebflux.metrics.METRICS_TAG_HTTP_STATUS
import com.example.demowebflux.metrics.METRICS_TAG_URI
import com.example.demowebflux.utils.JwtTestUtils
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.util.UriComponentsBuilder
import java.util.*

abstract class Proxy5xxErrorsTest(
    protected val httpMethod: HttpMethod,
    protected val uri: String,
    protected val validBody: String?,
    protected val clientUri: String,
) : AbstractMockWebServerTest() {
    private val metricsUri = UriComponentsBuilder.fromUriString(uri).replaceQuery(null).build().toUriString()

    @Test
    fun `502_whenConnectionException`() = runTest {
        webTestClient.method(httpMethod)
            .uri(uri)
            .header(HEADER_X_REQUEST_ID, UUID.randomUUID().toString())
            .header(HttpHeaders.AUTHORIZATION, JwtTestUtils.TOKEN)
            .let { if (validBody == null) it else it.bodyValue(validBody) }
            .assertError(502)
    }

    fun testSocketPolicy(): List<Arguments> = listOf(
        arguments(SocketPolicy.NO_RESPONSE, 504),
        arguments(SocketPolicy.DISCONNECT_AFTER_REQUEST, 502),
    )

    @ParameterizedTest
    @MethodSource
    fun testSocketPolicy(socketPolicy: SocketPolicy, httpStatus: Int) =
        mockWebServer(SERVER_PORT, object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setSocketPolicy(socketPolicy)
            }
        }) {
            webTestClient.method(httpMethod)
                .uri(uri)
                .header(HEADER_X_REQUEST_ID, UUID.randomUUID().toString())
                .header(HttpHeaders.AUTHORIZATION, JwtTestUtils.TOKEN)
                .let { if (validBody == null) it else it.bodyValue(validBody) }
                .assertError(httpStatus)
        }

    @Test
    fun `500_whenBodyTooLarge`() = mockWebServer(SERVER_PORT, object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return MockResponse().setBody(RandomStringUtils.randomAlphanumeric(2 * 1024))
        }
    }) {
        if (httpMethod == HttpMethod.GET) {
            return@mockWebServer
        }

        webTestClient.method(httpMethod)
            .uri(uri)
            .header(HEADER_X_REQUEST_ID, UUID.randomUUID().toString())
            .header(HttpHeaders.AUTHORIZATION, JwtTestUtils.TOKEN)
            .let { if (validBody == null) it else it.bodyValue(RandomStringUtils.randomAlphanumeric(5 * 1024)) }
            .assertError(500)
    }

    private fun WebTestClient.RequestHeadersSpec<*>.assertError(httpStatus: Int) {
        val rawResponse = this.exchange()
            .expectStatus()
            .isEqualTo(httpStatus)
            .expectBody<String>()
            .returnResult()
            .responseBody

        log.info("raw response: {}", rawResponse)

        assertMeter(
            DemoMetrics::httpServerRequests.name, mapOf(
                METRICS_TAG_HTTP_METHOD to httpMethod.name(),
                METRICS_TAG_URI to metricsUri,
            )
        )
        assertMeter(
            DemoMetrics::httpServerResponses.name, mapOf(
                METRICS_TAG_HTTP_METHOD to httpMethod.name(),
                METRICS_TAG_URI to metricsUri,
                METRICS_TAG_HTTP_STATUS to httpStatus.toString(),
            )
        )
        assertMeter(
            DemoMetrics::httpClientRequests.name, mapOf(
                METRICS_TAG_HTTP_METHOD to httpMethod.name(),
                METRICS_TAG_URI to "http://localhost:$SERVER_PORT$clientUri",
            )
        )
        assertMeter(
            DemoMetrics::httpClientResponses.name, mapOf(
                METRICS_TAG_HTTP_METHOD to httpMethod.name(),
                METRICS_TAG_URI to "http://localhost:$SERVER_PORT$clientUri",
                METRICS_TAG_HTTP_STATUS to "null",
            )
        )
    }
}
