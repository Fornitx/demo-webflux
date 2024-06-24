package com.example.demowebflux.rest.proxy

import com.example.demowebflux.AbstractMockWebServerTest
import com.example.demowebflux.constants.API_V2
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.metrics.METRICS_TAG_HTTP_METHOD
import com.example.demowebflux.metrics.METRICS_TAG_HTTP_STATUS
import com.example.demowebflux.metrics.METRICS_TAG_URI
import com.example.demowebflux.utils.JwtTestUtils
import com.example.demowebflux.utils.requestId
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import kotlin.test.assertEquals

private const val CUSTOM_REQUEST_HEADER = "custom-request-header"
private const val CUSTOM_REQUEST_HEADER_VALUE = CUSTOM_REQUEST_HEADER + "_1"
private const val CUSTOM_RESPONSE_HEADER = "custom-response-header"
private const val CUSTOM_RESPONSE_HEADER_VALUE = CUSTOM_RESPONSE_HEADER + "_1"

@SpringBootTest
class ProxySuccessMockWebServerTest : AbstractMockWebServerTest() {
    fun testSuccess(): List<MockResponse> = listOf(
        MockResponse().setResponseCode(200).setBody("""{"test":"test"}""")
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
        MockResponse().setResponseCode(400).setBody("123"),
        // bodiless response
        MockResponse().setResponseCode(500),
    ).map { it.setHeader(CUSTOM_RESPONSE_HEADER, CUSTOM_RESPONSE_HEADER_VALUE) }

    @ParameterizedTest
    @MethodSource("testSuccess")
    fun GET(mockResponse: MockResponse) = mockWebServer(SERVER_PORT, object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            assertEquals("GET", request.method)
            assertEquals("/testPath?testParam=123", request.path)
            assertEquals(CUSTOM_REQUEST_HEADER_VALUE, request.headers[CUSTOM_REQUEST_HEADER])
            return mockResponse
        }
    }) {
        val requestId = requestId()
        val expectedStatus = mockResponse.status.split(" ")[1]

        val responseBody = webTestClient.get()
            .uri("$API_V2/testPath?testParam=123")
            .header(AUTHORIZATION, JwtTestUtils.TOKEN)
            .header(HEADER_X_REQUEST_ID, requestId)
            .header(CUSTOM_REQUEST_HEADER, CUSTOM_REQUEST_HEADER_VALUE)
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus.toInt())
            .expectHeader()
            .valueEquals(HEADER_X_REQUEST_ID, requestId)
            .expectHeader()
            .valueEquals(CUSTOM_RESPONSE_HEADER, CUSTOM_RESPONSE_HEADER_VALUE)
            .expectBody<String>()
            .returnResult()
            .responseBody

        log.info { "responseBody = $responseBody" }

        assertEquals(mockResponse.getBody()?.readUtf8(), responseBody)
        assertSuccessMetrics("GET", expectedStatus)
    }

    @ParameterizedTest
    @MethodSource("testSuccess")
    fun POST(mockResponse: MockResponse) = mockWebServer(SERVER_PORT, object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            assertEquals("POST", request.method)
            assertEquals("/testPath?testParam=123", request.path)
            assertEquals(CUSTOM_REQUEST_HEADER_VALUE, request.headers[CUSTOM_REQUEST_HEADER])
            assertEquals("""{"abc":"123"}""", request.body.readUtf8())
            return mockResponse
        }
    }) {
        val requestId = requestId()
        val expectedStatus = mockResponse.status.split(" ")[1]

        val responseBody = webTestClient.post()
            .uri("$API_V2/testPath?testParam=123")
            .header(AUTHORIZATION, JwtTestUtils.TOKEN)
            .header(HEADER_X_REQUEST_ID, requestId)
            .header(CUSTOM_REQUEST_HEADER, CUSTOM_REQUEST_HEADER_VALUE)
            .bodyValue(mapOf("abc" to "123"))
            .exchange()
            .expectStatus()
            .isEqualTo(expectedStatus.toInt())
            .expectHeader()
            .valueEquals(HEADER_X_REQUEST_ID, requestId)
            .expectHeader()
            .valueEquals(CUSTOM_RESPONSE_HEADER, CUSTOM_RESPONSE_HEADER_VALUE)
            .expectBody<String>()
            .returnResult()
            .responseBody

        log.info { "responseBody = $responseBody" }

        assertEquals(mockResponse.getBody()?.readUtf8(), responseBody)
        assertSuccessMetrics("POST", expectedStatus)
    }

    private fun assertSuccessMetrics(httpMethod: String, httpStatus: String) {
        assertNoMeter(DemoMetrics::error.name)

        assertMeter(
            DemoMetrics::httpServerRequests.name, mapOf(
                METRICS_TAG_HTTP_METHOD to httpMethod,
                METRICS_TAG_URI to "$API_V2/testPath",
            )
        )
        assertMeter(
            DemoMetrics::httpServerResponses.name, mapOf(
                METRICS_TAG_HTTP_METHOD to httpMethod,
                METRICS_TAG_URI to "$API_V2/testPath",
                METRICS_TAG_HTTP_STATUS to httpStatus,
            )
        )
        assertMeter(
            DemoMetrics::httpClientRequests.name, mapOf(
                METRICS_TAG_HTTP_METHOD to httpMethod,
                METRICS_TAG_URI to "http://localhost:$SERVER_PORT/testPath",
            )
        )
        assertMeter(
            DemoMetrics::httpClientResponses.name, mapOf(
                METRICS_TAG_HTTP_METHOD to httpMethod,
                METRICS_TAG_URI to "http://localhost:$SERVER_PORT/testPath",
                METRICS_TAG_HTTP_STATUS to httpStatus,
            )
        )
    }
}
