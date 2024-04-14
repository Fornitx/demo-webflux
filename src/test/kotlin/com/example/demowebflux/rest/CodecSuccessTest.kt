package com.example.demowebflux.rest

import com.example.demowebflux.AbstractLoggingTest
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.constants.PATH_API_V1
import com.example.demowebflux.constants.PREFIX
import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.metrics.METRICS_TAG_PATH
import com.example.demowebflux.utils.JwtTestUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.util.TestSocketUtils
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.util.StringUtils
import java.nio.charset.Charset
import java.util.*
import kotlin.test.assertEquals

@SpringBootTest(
    properties = [
        "spring.codec.max-in-memory-size=10MB"
    ]
)
@AutoConfigureWebTestClient
@DirtiesContext
class CodecSuccessTest : AbstractLoggingTest() {
    companion object {
        val SERVER_PORT = TestSocketUtils.findAvailableTcpPort()

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("$PREFIX.service.multiplier") { "6" }
            registry.add("$PREFIX.client.url") { "http://localhost:$SERVER_PORT" }
        }
    }

    @Autowired
    private lateinit var client: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val validPath = "$PATH_API_V1/foo/12?fooNewId=16"
    private val validBody = DemoRequest("abc", others = mapOf("a" to "b"))

    @Test
    fun `200 OK`() = mockWebServer(SERVER_PORT, object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val body = request.body.readString(Charset.defaultCharset())
            return MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .setBody(StringUtils.capitalize(body))
        }
    }) {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client
            .post()
            .uri(validPath)
            .header(AUTHORIZATION, JwtTestUtils.TOKEN)
            .header(HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .valueEquals(HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertLogger(3)

        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<DemoResponse>(rawResponse!!)

        log.info { "response: $response" }

        assertEquals("Abc".repeat(6), response.msg)

        assertNoMeter(DemoMetrics::error.name)
        assertMeter(DemoMetrics::httpTimings.name, mapOf(METRICS_TAG_PATH to "$PATH_API_V1/foo/12"))
    }

    @Test
    fun `200 CODEC_SIZE`() = mockWebServer(SERVER_PORT, object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return MockResponse().setBody(RandomStringUtils.randomAlphanumeric(1024 * 1024))
        }
    }) {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client
            .post()
            .uri(validPath)
            .header(AUTHORIZATION, JwtTestUtils.TOKEN)
            .header(HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody.copy(nullOrNotBlankStr = "ABC"))
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .valueEquals(HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertLogger(3)

        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<DemoResponse>(rawResponse!!)

        log.info { "response: $response" }

//        assertEquals("Abc".repeat(6), response.msg)

        assertNoMeter(DemoMetrics::error.name)
        assertMeter(DemoMetrics::httpTimings.name, mapOf(METRICS_TAG_PATH to "$PATH_API_V1/foo/12"))
    }

    private fun mockWebServer(port: Int, dispatcher: Dispatcher, function: suspend () -> Unit) =
        MockWebServer().use { server ->
            server.dispatcher = dispatcher
            server.start(port)
            runTest {
                function()
            }
        }
}
