package com.example.demowebflux.rest

import com.example.demowebflux.AbstractLoggingTest
import com.example.demowebflux.constants.API_V1
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.constants.PREFIX
import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.metrics.METRICS_TAG_CODE
import com.example.demowebflux.metrics.METRICS_TAG_HTTP_STATUS
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
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.util.TestSocketUtils
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.util.*

@SpringBootTest
@AutoConfigureWebTestClient
class CodecErrorTest : AbstractLoggingTest() {
    companion object {
        val SERVER_PORT = TestSocketUtils.findAvailableTcpPort()

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("$PREFIX.service.multiplier") { "10" }
            registry.add("$PREFIX.client.url") { "http://localhost:$SERVER_PORT" }
        }
    }

    @Autowired
    private lateinit var client: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val validPath = "$API_V1/foo/12?fooNewId=16"
    private val validBody = DemoRequest("abc", others = mapOf("a" to "b"))

    @Test
    fun error() = mockWebServer(SERVER_PORT, object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return MockResponse().setBody(RandomStringUtils.randomAlphanumeric(1024 * 1024))
        }
    }) {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client.post()
            .uri(validPath)
            .header(AUTHORIZATION, JwtTestUtils.TOKEN)
            .header(HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody.copy(nullOrNotBlankStr = "ABC"))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .valueEquals(HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertLogger(4)

        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<Map<*, *>>(rawResponse!!)

        log.info { "response: $response" }

        assertNoMeter(DemoMetrics::httpServerRequests.name)
        assertMeter(
            DemoMetrics::error.name, mapOf(
                METRICS_TAG_CODE to DemoError.UNEXPECTED_5XX_ERROR.code.toString(),
                METRICS_TAG_HTTP_STATUS to DemoError.UNEXPECTED_5XX_ERROR.httpStatus.toString()
            )
        )
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
