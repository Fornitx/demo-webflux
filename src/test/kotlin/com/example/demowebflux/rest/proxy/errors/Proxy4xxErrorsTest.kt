package com.example.demowebflux.rest.proxy.errors

import com.example.demowebflux.AbstractWebTestClientTest
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.metrics.METRICS_TAG_HTTP_METHOD
import com.example.demowebflux.metrics.METRICS_TAG_HTTP_STATUS
import com.example.demowebflux.metrics.METRICS_TAG_URI
import com.example.demowebflux.utils.JwtTestUtils
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.util.UriComponentsBuilder
import java.util.*

abstract class Proxy4xxErrorsTest(
    protected val httpMethod: HttpMethod,
    protected val uri: String,
    protected val validBody: String?,
) : AbstractWebTestClientTest() {
    private val metricsUri = UriComponentsBuilder.fromUriString(uri).replaceQuery(null).build().toUriString()

    @Test
    fun `400_whenNoRequestIdHeader`() = runTest {
        webTestClient.method(httpMethod)
            .uri(uri)
            .let { if (validBody == null) it else it.bodyValue(validBody) }
            .assertError(400)
    }

    @Test
    fun `401_whenNoAuthHeader`() = runTest {
        webTestClient.method(httpMethod)
            .uri(uri)
            .header(HEADER_X_REQUEST_ID, UUID.randomUUID().toString())
            .let { if (validBody == null) it else it.bodyValue(validBody) }
            .assertError(401)
    }

    @Test
    fun `401_whenTokenIsOverdue`() = runTest {
        webTestClient.method(httpMethod)
            .uri(uri)
            .header(HEADER_X_REQUEST_ID, UUID.randomUUID().toString())
            .header(HttpHeaders.AUTHORIZATION, JwtTestUtils.TOKEN_OVERDUE)
            .let { if (validBody == null) it else it.bodyValue(validBody) }
            .assertError(401)
    }

    @Test
    fun `401_whenTokenWithBadSign`() = runTest {
        webTestClient.method(httpMethod)
            .uri(uri)
            .header(HEADER_X_REQUEST_ID, UUID.randomUUID().toString())
            .header(HttpHeaders.AUTHORIZATION, JwtTestUtils.TOKEN_BAD_SIGN)
            .let { if (validBody == null) it else it.bodyValue(validBody) }
            .assertError(401)
    }

    @Test
    fun `403_whenTokenWithBadAudience`() = runTest {
        webTestClient.method(httpMethod)
            .uri(uri)
            .header(HEADER_X_REQUEST_ID, UUID.randomUUID().toString())
            .header(HttpHeaders.AUTHORIZATION, JwtTestUtils.TOKEN_NO_AUD)
            .let { if (validBody == null) it else it.bodyValue(validBody) }
            .assertError(403)
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
    }
}
