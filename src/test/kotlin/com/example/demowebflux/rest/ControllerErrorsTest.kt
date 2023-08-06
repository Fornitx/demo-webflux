package com.example.demowebflux.rest

import com.example.demowebflux.AbstractLoggingTest
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.constants.PATH_V1
import com.example.demowebflux.data.DemoErrorResponse
import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.metrics.METRICS_TAG_CODE
import com.example.demowebflux.metrics.METRICS_TAG_STATUS
import com.example.demowebflux.rest.client.DemoClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.util.*
import kotlin.test.assertContains
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureWebTestClient
@DirtiesContext
class ControllerErrorsTest : AbstractLoggingTest() {
    @Autowired
    private lateinit var client: WebTestClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var clientMock: DemoClient

    private val validPath = PATH_V1 + "/foo/12"
    private val validBody = DemoRequest("abc", others = mapOf("a" to "b"))

    @Test
    fun `405 MethodNotAllowed when method is put`() {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client.put()
            .uri(validPath)
            .header(HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .valueEquals(HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertLogger(2)
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
            .header(HEADER_X_REQUEST_ID, requestId)
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue(objectMapper.writeValueAsString(validBody))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .valueEquals(HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertLogger(2)
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
            .header(HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody)
            .accept(MediaType.TEXT_PLAIN)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.NOT_ACCEPTABLE)
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .valueEquals(HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertLogger(2)
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
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .doesNotExist(HEADER_X_REQUEST_ID)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertLogger(2)
        assertRawResponse(
            rawResponse,
            DemoError.UNEXPECTED_4XX_ERROR,
            "Required header '${HEADER_X_REQUEST_ID}' is not present"
        )
    }

    @Test
    fun `400 BadRequest when request is invalid JSON`() {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client.post()
            .uri(validPath)
            .header(HEADER_X_REQUEST_ID, requestId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{{}".trim())
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .valueEquals(HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertLogger(2)
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
            .header(HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody.copy(msg = "ab"))
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .valueEquals(HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertLogger(2)
        assertRawResponse(
            rawResponse,
            DemoError.UNEXPECTED_4XX_ERROR,
            "Field error in object 'demoRequest' on field 'msg': rejected value [ab]",
            "size must be between 3 and 256"
        )
    }

    @Test
    fun `400 BadRequest when request tags is empty (custom validator)`() {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client.post()
            .uri(validPath)
            .header(HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody.copy(tags = setOf()))
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .valueEquals(HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertLogger(2)
        assertRawResponse(
            rawResponse,
            DemoError.UNEXPECTED_4XX_ERROR,
            "Field error in object 'demoRequest' on field 'tags': rejected value",
            "default message [Value must be null or not empty]"
        )
    }

    @Test
    fun `409 Conflict when request message is 666`() {
        val requestId = UUID.randomUUID().toString()
        val rawResponse = client.post()
            .uri(validPath)
            .header(HEADER_X_REQUEST_ID, requestId)
            .bodyValue(validBody.copy(msg = "666"))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectHeader()
            .valueEquals(HEADER_X_REQUEST_ID, requestId)
            .expectBody<String>()
            .returnResult()
            .responseBody

        assertLogger(3)
        assertRawResponse(rawResponse, DemoError.MSG_IS_666, DemoError.MSG_IS_666.message)
    }

    private fun assertRawResponse(rawResponse: String?, demoError: DemoError, vararg messages: String) {
        log.info { "raw response: $rawResponse" }

        val response = objectMapper.readValue<DemoErrorResponse>(rawResponse!!)

        log.info { "response: $response" }

        // TODO check status
        assertEquals(demoError.code, response.code)
        for (message in messages) {
            assertContains(response.detailedMessage!!, message)
        }

        verifyNoInteractions(clientMock)

        assertNoMeter(DemoMetrics::httpTimings.name)
        assertMeter(
            DemoMetrics::error.name, mapOf(
                METRICS_TAG_CODE to demoError.code.toString(),
                METRICS_TAG_STATUS to demoError.httpStatus.toString()
            )
        )
    }
}
