package com.example.demowebflux.rest

import com.example.demowebflux.AbstractMetricsTest
import com.example.demowebflux.data.DemoErrorResponse
import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.metrics.METRICS_TAG_CODE
import com.example.demowebflux.utils.Constants
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.util.*

@SpringBootTest
@AutoConfigureWebTestClient
class ControllerErrorsTest : AbstractMetricsTest() {
    @Autowired
    private lateinit var client: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var clientMock: DemoClient

    private val validPath = Constants.PATH_V1 + "/foo/12"
    private val validBody = DemoRequest("123", others = mapOf("a" to "b"))

    @Test
    fun `405 MethodNotAllowed when method is put`() {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client.put()
            .uri(validPath)
            .header(Constants.HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
            .expectHeader()
            .valueEquals(Constants.HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertRawResponse(
            rawResponse,
            DemoError.UNEXPECTED_4XX_ERROR,
            "Request method 'PUT' is not supported"
        )
    }

    @Test
    fun `415 UnsupportedMediaType when contentType is not JSON`() {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client.post()
            .uri(validPath)
            .header(Constants.HEADER_X_REQUEST_ID, requestId)
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue(objectMapper.writeValueAsString(validBody))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .expectHeader()
            .valueEquals(Constants.HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertRawResponse(
            rawResponse,
            DemoError.UNEXPECTED_4XX_ERROR,
            "415 UNSUPPORTED_MEDIA_TYPE",
            "Content type 'text/plain' not supported",
        )
    }

    @Test
    fun `406 NotAcceptable when acceptType is not JSON`() {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client.post()
            .uri(validPath)
            .header(Constants.HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody)
            .accept(MediaType.TEXT_PLAIN)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.NOT_ACCEPTABLE)
            .expectHeader()
            .valueEquals(Constants.HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertRawResponse(
            rawResponse,
            DemoError.UNEXPECTED_4XX_ERROR,
            "406 NOT_ACCEPTABLE",
            "Could not find acceptable representation",
        )
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

        assertRawResponse(
            rawResponse,
            DemoError.UNEXPECTED_4XX_ERROR,
            "Required header '${Constants.HEADER_X_REQUEST_ID}' is not present"
        )
    }

    @Test
    fun `400 BadRequest when request is invalid JSON`() {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client.post()
            .uri(validPath)
            .header(Constants.HEADER_X_REQUEST_ID, requestId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{{}".trim())
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectHeader()
            .valueEquals(Constants.HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertRawResponse(
            rawResponse,
            DemoError.UNEXPECTED_4XX_ERROR,
            "400 BAD_REQUEST",
            "Failed to read HTTP message"
        )
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

        assertRawResponse(
            rawResponse,
            DemoError.UNEXPECTED_4XX_ERROR,
            "Field error in object 'demoRequest' on field 'msg': rejected value [12]",
            "size must be between 3 and 2147483647"
        )
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

        assertRawResponse(
            rawResponse,
            DemoError.UNEXPECTED_4XX_ERROR,
            "Field error in object 'demoRequest' on field 'tags': rejected value [[]]",
            "{javax.validation.constraints.NullOrNotEmpty.message}"
        )
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

        assertRawResponse(rawResponse, DemoError.MSG_IS_666, DemoError.MSG_IS_666.message)
    }

    private fun assertRawResponse(rawResponse: String?, demoError: DemoError, vararg messages: String) {
        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<DemoErrorResponse>(rawResponse!!)

        log.info { "response: $response" }

        // TODO check status
        assertThat(response.code).isEqualTo(demoError.code)
        for (message in messages) {
            assertThat(response.detailedMessage).contains(message)
        }

        verifyNoInteractions(clientMock)

        assertNoMeter(DemoMetrics::httpTimings.name)
        assertMeter(DemoMetrics::error.name, mapOf(METRICS_TAG_CODE to demoError.code.toString()))
    }
}