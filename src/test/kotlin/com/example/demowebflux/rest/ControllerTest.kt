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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
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

    @Test
    fun `when all valid then 200 OK`() {
        val rawResponse = client/*.mutate()
            .responseTimeout(Duration.ofHours(1))
            .build()*/
            .post()
            .uri(Constants.PATH_V1 + "/foo/12")
            .header(Constants.HEADER_X_REQUEST_ID, UUID.randomUUID().toString())
            .bodyValue(DemoRequest("123", others = mapOf("a" to "b")))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<String>()
            .returnResult()
            .responseBody

        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<DemoResponse>(rawResponse!!)

        log.info { "response: $response" }

        assertThat(response.msg).isEqualTo("123".repeat(3))

        assertMeter(DemoMetrics::httpTimings.name, mapOf(METRICS_TAG_PATH to "/v1/foo/12"))
    }

    @Test
    fun `when request fails javax validation then 400 BadRequest`() {
        val rawResponse = client.post()
            .uri(Constants.PATH_V1 + "/foo/12")
            .header(Constants.HEADER_X_REQUEST_ID, UUID.randomUUID().toString())
            .bodyValue(DemoRequest("12", others = mapOf("a" to "b")))
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody<String>()
            .returnResult()
            .responseBody

        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<DemoErrorResponse>(rawResponse!!)

        log.info { "response: $response" }

        assertThat(response.detailedMessage)
            .contains("Field error in object 'demoRequest' on field 'msg': rejected value [12]")
            .contains("size must be between 3 and 2147483647")

        assertNoMeter(DemoMetrics::httpTimings.name)
        assertMeter(DemoMetrics::error.name, mapOf(METRICS_TAG_CODE to DemoError.UNEXPECTED_400_ERROR.code.toString()))
    }

    @Test
    fun `when request tags is empty then 400 BadRequest`() {
        val rawResponse = client.post()
            .uri(Constants.PATH_V1 + "/foo/12")
            .header(Constants.HEADER_X_REQUEST_ID, UUID.randomUUID().toString())
            .bodyValue(DemoRequest("123", setOf(), others = mapOf("a" to "b")))
            .exchange()
            .expectStatus()
            .isBadRequest
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

        assertNoMeter(DemoMetrics::httpTimings.name)
        assertMeter(DemoMetrics::error.name, mapOf(METRICS_TAG_CODE to DemoError.UNEXPECTED_400_ERROR.code.toString()))
    }

    @Test
    fun `when request message is 666 then 409 Conflict`() {
        val rawResponse = client.post()
            .uri(Constants.PATH_V1 + "/foo/12")
            .header(Constants.HEADER_X_REQUEST_ID, UUID.randomUUID().toString())
            .bodyValue(DemoRequest("666", others = mapOf("a" to "b")))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
            .expectBody<String>()
            .returnResult()
            .responseBody

        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<DemoErrorResponse>(rawResponse!!)

        log.info { "response: $response" }

        assertThat(response.code).isEqualTo(DemoError.MSG_IS_666.code)
        assertThat(response.detailedMessage).isEqualTo(DemoError.MSG_IS_666.message)

        assertNoMeter(DemoMetrics::httpTimings.name)
        assertMeter(DemoMetrics::error.name, mapOf(METRICS_TAG_CODE to DemoError.MSG_IS_666.code.toString()))
    }
}
