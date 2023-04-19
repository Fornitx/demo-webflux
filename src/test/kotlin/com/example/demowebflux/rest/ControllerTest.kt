package com.example.demowebflux.rest

import com.example.demowebflux.AbstractMetricsTest
import com.example.demowebflux.data.DemoErrorResponse
import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.metrics.METRICS_TAG_CODE
import com.example.demowebflux.metrics.METRICS_TAG_PATH
import com.example.demowebflux.utils.Constants
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.util.*

@SpringBootTest
@AutoConfigureWebTestClient
class ControllerTest : AbstractMetricsTest() {
    @Autowired
    private lateinit var client: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var clientMock: DemoClient

    private val validPath = Constants.PATH_V1 + "/foo/12"
    private val validBody = DemoRequest("123", others = mapOf("a" to "b"))

    @Test
    fun `200 OK`() = runTest {
        whenever(clientMock.call(any())).then { (it.arguments.first() as String).repeat(6) }

        val requestId = UUID.randomUUID().toString()
        val rawResponse = client/*.mutate()
            .responseTimeout(Duration.ofHours(1))
            .build()*/
            .post()
            .uri(validPath)
            .header(Constants.HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .valueEquals(Constants.HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<DemoResponse>(rawResponse!!)

        log.info { "response: $response" }

        assertThat(response.msg).isEqualTo("123".repeat(6))

        verify(clientMock).call(validBody.msg)

        assertNoMeter(DemoMetrics::error.name)
        assertMeter(DemoMetrics::httpTimings.name, mapOf(METRICS_TAG_PATH to "/v1/foo/12"))
    }

    @Test
    fun `400 BadRequest when request header is missing`() {
        val rawResponse = client.post()
            .uri(validPath)
            .bodyValue(validBody)
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectHeader()
            .doesNotExist(Constants.HEADER_X_REQUEST_ID)
            .expectBody<String>()
            .returnResult()
            .responseBody

        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<DemoErrorResponse>(rawResponse!!)

        log.info { "response: $response" }

        assertThat(response.detailedMessage)
            .contains("Required header '${Constants.HEADER_X_REQUEST_ID}' is not present")

        verifyNoInteractions(clientMock)

        assertNoMeter(DemoMetrics::httpTimings.name)
        assertMeter(DemoMetrics::error.name, mapOf(METRICS_TAG_CODE to DemoError.UNEXPECTED_400_ERROR.code.toString()))
    }

    @Test
    fun `400 BadRequest when request fails javax validation`() {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client.post()
            .uri(validPath)
            .header(Constants.HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody.copy(msg = "12"))
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectHeader()
            .valueEquals(Constants.HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<DemoErrorResponse>(rawResponse!!)

        log.info { "response: $response" }

        assertThat(response.detailedMessage)
            .contains("Field error in object 'demoRequest' on field 'msg': rejected value [12]")
            .contains("size must be between 3 and 2147483647")

        verifyNoInteractions(clientMock)

        assertNoMeter(DemoMetrics::httpTimings.name)
        assertMeter(DemoMetrics::error.name, mapOf(METRICS_TAG_CODE to DemoError.UNEXPECTED_400_ERROR.code.toString()))
    }

    @Test
    fun `400 BadRequest when request tags is empty (custom validator)`() {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client.post()
            .uri(validPath)
            .header(Constants.HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody.copy(tags = setOf()))
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectHeader()
            .valueEquals(Constants.HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<DemoErrorResponse>(rawResponse!!)

        log.info { "response: $response" }

        assertThat(response.code).isEqualTo(DemoError.UNEXPECTED_400_ERROR.code)
        assertThat(response.detailedMessage)
            .contains("Field error in object 'demoRequest' on field 'tags': rejected value [[]]")
            .contains("{javax.validation.constraints.NullOrNotEmpty.message}")

        verifyNoInteractions(clientMock)

        assertNoMeter(DemoMetrics::httpTimings.name)
        assertMeter(DemoMetrics::error.name, mapOf(METRICS_TAG_CODE to DemoError.UNEXPECTED_400_ERROR.code.toString()))
    }

    @Test
    fun `409 Conflict when request message is 666`() {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client.post()
            .uri(validPath)
            .header(Constants.HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody.copy(msg = "666"))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
            .expectHeader()
            .valueEquals(Constants.HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<DemoErrorResponse>(rawResponse!!)

        log.info { "response: $response" }

        assertThat(response.code).isEqualTo(DemoError.MSG_IS_666.code)
        assertThat(response.detailedMessage).isEqualTo(DemoError.MSG_IS_666.message)

        verifyNoInteractions(clientMock)

        assertNoMeter(DemoMetrics::httpTimings.name)
        assertMeter(DemoMetrics::error.name, mapOf(METRICS_TAG_CODE to DemoError.MSG_IS_666.code.toString()))
    }
}
