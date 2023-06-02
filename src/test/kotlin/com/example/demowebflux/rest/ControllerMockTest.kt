package com.example.demowebflux.rest

import com.example.demowebflux.AbstractLoggingTest
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.constants.PATH_V1
import com.example.demowebflux.constants.PREFIX
import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.metrics.METRICS_TAG_PATH
import com.example.demowebflux.rest.client.DemoClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.util.StringUtils
import java.util.*

@SpringBootTest(properties = [
    "$PREFIX.service.multiplier=6"
])
@AutoConfigureWebTestClient
@DirtiesContext
class ControllerMockTest : AbstractLoggingTest() {
    @Autowired
    private lateinit var client: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var clientMock: DemoClient

    private val validPath = PATH_V1 + "/foo/12"
    private val validBody = DemoRequest("abc", others = mapOf("a" to "b"))

    @Test
    fun `200 OK`() = runTest {
        whenever(clientMock.call(any())).then { StringUtils.capitalize(it.arguments.first() as String) }

        val requestId = UUID.randomUUID().toString()
        val rawResponse = client/*.mutate()
            .responseTimeout(Duration.ofHours(1))
            .build()*/
            .post()
            .uri(validPath)
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

        assertThat(response.msg).isEqualTo("Abc".repeat(6))

        verify(clientMock).call(validBody.msg)

        assertNoMeter(DemoMetrics::error.name)
        assertMeter(DemoMetrics::httpTimings.name, mapOf(METRICS_TAG_PATH to "$PATH_V1/foo/12"))

        assertMeter(DemoMetrics::cacheHits.name, mapOf(), 0)
        assertMeter(DemoMetrics::cacheMiss.name, mapOf(), 1)
    }
}
