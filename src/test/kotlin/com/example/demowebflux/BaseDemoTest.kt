package com.example.demowebflux

import com.example.demowebflux.data.DemoErrorResponse
import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.example.demowebflux.error.PredictableException
import com.example.demowebflux.filters.TraceIdFilter.Companion.TRACE_ID_HEADER
import mu.KLoggable
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@TestInstance(PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
abstract class BaseDemoTest(
    protected val PATH: String,
    protected val MSG: String,
) : KLoggable {
    companion object {
        protected fun randomString(): String = RandomStringUtils.randomAlphanumeric(12)
    }

    override val logger = logger()

    @Autowired
    private lateinit var client: WebTestClient

    @SpyBean
    private lateinit var service: DemoService

    protected abstract val happyWayMsg: String

    @Order(1)
    @Test
    open fun testHappyWay() {
        val traceId = randomString()
        val request = DemoRequest(MSG, _anyField = mapOf("A" to mapOf("B" to "C")))

        client.post()
            .uri(PATH)
            .header(TRACE_ID_HEADER, traceId)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .valueEquals(TRACE_ID_HEADER, traceId)
            .expectBody(DemoResponse::class.java)
            .consumeWith {
                val response = it.responseBody!!
                logger.info { response }
                assertThat(response.msg).isEqualTo(happyWayMsg)
                assertThat(response._anyField).isEqualTo(request._anyField)
            }
    }

    @Order(2)
    @Test
    fun testAnotherPath() {
        client.post()
            .uri(PATH + "XXX")
            .bodyValue(DemoRequest(randomString()))
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody(String::class.java)
            .consumeWith {
                val response = it.responseBody!!
                logger.error { response }
                assertThat(response).contains(""""path":"${PATH}XXX"""")
            }
    }

    @Order(3)
    @Test
    fun testNoHeader() {
        val status = HttpStatus.BAD_REQUEST

        client.post()
            .uri(PATH)
            .bodyValue(DemoRequest(randomString()))
            .exchange()
            .expectStatus()
            .isEqualTo(status)
            .expectHeader()
            .doesNotExist(TRACE_ID_HEADER)
            .expectBody(DemoErrorResponse::class.java)
            .consumeWith {
                val response = it.responseBody!!
                logger.error { response }
                assertThat(response.status.code).isEqualTo(status.value())
            }
    }

    @Order(4)
    @Test
    fun testBadContentType() {
        val traceId = randomString()
        val status = HttpStatus.UNSUPPORTED_MEDIA_TYPE

        client.post()
            .uri(PATH)
            .header(TRACE_ID_HEADER, traceId)
            .bodyValue(MSG)
            .exchange()
            .expectStatus()
            .isEqualTo(status)
            .expectHeader()
            .valueEquals(TRACE_ID_HEADER, traceId)
            .expectBody(DemoErrorResponse::class.java)
            .consumeWith {
                val response = it.responseBody!!
                logger.error { response }
                assertThat(response.status.code).isEqualTo(status.value())
            }
    }

    @Order(5)
    @Test
    fun testBadAcceptType() {
        val traceId = randomString()
        val status = HttpStatus.NOT_ACCEPTABLE

        client.post()
            .uri(PATH)
            .header(TRACE_ID_HEADER, traceId)
            .accept(MediaType.TEXT_PLAIN)
            .bodyValue(DemoRequest(MSG))
            .exchange()
            .expectStatus()
            .isEqualTo(status)
            .expectHeader()
            .valueEquals(TRACE_ID_HEADER, traceId)
            .expectBody(DemoErrorResponse::class.java)
            .consumeWith {
                val response = it.responseBody!!
                logger.error { response }
                assertThat(response.status.code).isEqualTo(status.value())
            }
    }

    @Order(6)
    @Test
    fun testValidation() {
        val traceId = randomString()
        val status = HttpStatus.BAD_REQUEST

        client.post()
            .uri(PATH)
            .header(TRACE_ID_HEADER, traceId)
            .bodyValue(DemoRequest(MSG.substring(0..1)))
            .exchange()
            .expectStatus()
            .isEqualTo(status)
            .expectHeader()
            .valueEquals(TRACE_ID_HEADER, traceId)
            .expectBody(DemoErrorResponse::class.java)
            .consumeWith {
                val response = it.responseBody!!
                logger.error { response }
                assertThat(response.status.code).isEqualTo(status.value())
            }
    }

    @Order(7)
    @Test
    fun testInternalError() {
        val traceId = randomString()
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        whenever(service.foo(MSG)).thenReturn(Mono.error(RuntimeException("Unexpected error!")))
        whenever(service.bar(MSG)).thenReturn(Mono.error(RuntimeException("Unexpected error!")))

        client.post()
            .uri(PATH)
            .header(TRACE_ID_HEADER, traceId)
            .bodyValue(DemoRequest(MSG))
            .exchange()
            .expectStatus()
            .isEqualTo(status)
            .expectHeader()
            .valueEquals(TRACE_ID_HEADER, traceId)
            .expectBody(DemoErrorResponse::class.java)
            .consumeWith {
                val response = it.responseBody!!
                logger.info { response }
                assertThat(response.status.code).isEqualTo(status.value())
            }
    }

    @Order(8)
    @Test
    open fun testPredictableError() {
        val traceId = randomString()

        whenever(service.foo(MSG)).thenReturn(Mono.error(PredictableException("It's OK!")))
        whenever(service.bar(MSG)).thenReturn(Mono.error(PredictableException("It's OK!")))

        client.post()
            .uri(PATH)
            .header(TRACE_ID_HEADER, traceId)
            .bodyValue(DemoRequest(MSG))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.OK)
            .expectHeader()
            .valueEquals(TRACE_ID_HEADER, traceId)
            .expectBody(DemoErrorResponse::class.java)
            .consumeWith {
                val response = it.responseBody!!
                logger.info { response }
                assertThat(response.status.code).isEqualTo(PredictableException.HTTP_STATUS)
            }
    }
}
