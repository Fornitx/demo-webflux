package com.example.demowebflux.rest

import com.example.demowebflux.AbstractLoggingTest
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.constants.PATH_V1
import com.example.demowebflux.constants.PREFIX
import com.example.demowebflux.data.DemoErrorResponse
import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.metrics.METRICS_TAG_CODE
import com.example.demowebflux.metrics.METRICS_TAG_STATUS
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.test.runTest
import mockwebserver3.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.util.TestSocketUtils
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.util.*

@SpringBootTest
@AutoConfigureWebTestClient
@DirtiesContext
class ClientMockWebServerErrorsTest : AbstractLoggingTest() {
    companion object {
        val SERVER_PORT = TestSocketUtils.findAvailableTcpPort()

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("$PREFIX.client.url") { "http://localhost:$SERVER_PORT" }
        }
    }

    @Autowired
    private lateinit var client: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val validPath = PATH_V1 + "/foo/12"
    private val validBody = DemoRequest("abc", others = mapOf("a" to "b"))

    fun `500 INTERNAL_SERVER_ERROR`(): List<SocketPolicy> = listOf(
        SocketPolicy.DISCONNECT_AT_START,
        SocketPolicy.DISCONNECT_AFTER_REQUEST,
        SocketPolicy.DISCONNECT_AT_END,
        SocketPolicy.DISCONNECT_DURING_REQUEST_BODY,
        SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY,
    )

    @ParameterizedTest
    @MethodSource
    fun `500 INTERNAL_SERVER_ERROR`(socketPolicy: SocketPolicy) = mockWebServer(
        SERVER_PORT, MockResponse().setSocketPolicy(socketPolicy)
    ) {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client
            .post()
            .uri(validPath)
            .header(HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody)
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .valueEquals(HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertLogger(3)

        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<DemoErrorResponse>(rawResponse!!)

        log.info { "response: $response" }

        assertNoMeter(DemoMetrics::httpTimings.name)
        assertMeter(
            DemoMetrics::error.name, mapOf(
                METRICS_TAG_CODE to DemoError.UNEXPECTED_5XX_ERROR.code.toString(),
                METRICS_TAG_STATUS to DemoError.UNEXPECTED_5XX_ERROR.httpStatus.toString(),
            )
        )
    }

    private fun mockWebServer(port: Int, response: MockResponse, function: suspend () -> Unit) =
        MockWebServer().use { server ->
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return response
                }
            }
            server.start(port)
            runTest {
                function()
            }
        }
}
