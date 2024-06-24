//package com.example.demowebflux.rest
//
//import com.example.demowebflux.AbstractLoggingTest
//import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
//import com.example.demowebflux.constants.PATH_API_V1
//import com.example.demowebflux.constants.PREFIX
//import com.example.demowebflux.data.DemoRequest
//import com.example.demowebflux.data.DemoResponse
//import com.example.demowebflux.metrics.DemoMetrics
//import com.example.demowebflux.metrics.METRICS_TAG_URI
//import com.example.demowebflux.rest.client.DemoClient
//import com.example.demowebflux.utils.JwtTestUtils
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.module.kotlin.readValue
//import kotlinx.coroutines.test.runTest
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.Test
//import org.mockito.Mockito
//import org.mockito.invocation.InvocationOnMock
//import org.mockito.kotlin.any
//import org.mockito.kotlin.argumentCaptor
//import org.mockito.kotlin.verify
//import org.mockito.kotlin.whenever
//import org.mockito.stubbing.Answer
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.test.context.TestConfiguration
//import org.springframework.boot.test.mock.mockito.SpyBean
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Primary
//import org.springframework.http.HttpHeaders.AUTHORIZATION
//import org.springframework.http.MediaType
//import org.springframework.http.ResponseEntity
//import org.springframework.http.server.reactive.ServerHttpRequest
//import org.springframework.test.web.reactive.server.WebTestClient
//import org.springframework.test.web.reactive.server.expectBody
//import org.springframework.util.StringUtils
//import java.util.*
//import kotlin.test.assertEquals
//
//@SpringBootTest(properties = [
//    "$PREFIX.service.multiplier=6"
//])
//@AutoConfigureWebTestClient
//class MockitoSpyCaptorTest : AbstractLoggingTest() {
//    @Autowired
//    private lateinit var client: WebTestClient
//
//    @Autowired
//    private lateinit var objectMapper: ObjectMapper
//
//    @SpyBean
//    private lateinit var clientSpy: DemoClient
//
//    private val validPath = "$PATH_API_V1/foo/12?fooNewId=14"
//    private val validBody = DemoRequest("abc", others = mapOf("a" to "b"))
//
//    @Test
//    fun `200 OK`() = runTest {
//        val resultCaptor = ResultCaptor<String>()
//        Mockito.doAnswer(resultCaptor).whenever(clientSpy).foo(any())
//
//        val requestId = UUID.randomUUID().toString()
//        val rawResponse = client
//            .post()
//            .uri(validPath)
//            .header(AUTHORIZATION, JwtTestUtils.TOKEN)
//            .header(HEADER_X_REQUEST_ID, requestId)
//            .bodyValue(validBody)
//            .exchange()
//            .expectStatus()
//            .isOk
//            .expectHeader()
//            .contentType(MediaType.APPLICATION_JSON)
//            .expectHeader()
//            .valueEquals(HEADER_X_REQUEST_ID, requestId)
//            .expectBody<String>()
//            .returnResult()
//            .responseBody
//
//        assertLogger(4)
//
//        log.info { "raw response: $rawResponse" }
//
//        val response = objectMapper.readValue<DemoResponse>(rawResponse!!)
//
//        log.info { "response: $response" }
//
//        assertEquals("Abc".repeat(6), response.msg)
//
//        val argumentCaptor = argumentCaptor<String>()
//        verify(clientSpy).foo(argumentCaptor.capture())
//        assertThat(argumentCaptor.allValues)
//            .hasSize(1)
//            .first()
//            .isEqualTo(validBody.nullOrNotBlankStr)
//
//        assertEquals("Abc", resultCaptor.result)
//
//        assertNoMeter(DemoMetrics::error.name)
//        assertMeter(DemoMetrics::httpServerRequest.name, mapOf(METRICS_TAG_URI to "$PATH_API_V1/foo/12"))
//    }
//
//    class ResultCaptor<T> : Answer<Any?> {
//        var result: T? = null
//            private set
//
//        override fun answer(invocationOnMock: InvocationOnMock): T? {
//            result = invocationOnMock.callRealMethod() as T?
//            return result
//        }
//    }
//
//    @TestConfiguration
//    class MyConfiguration {
//        @Primary
//        @Bean
//        fun demoClient(): DemoClient {
//            return object : DemoClient {
//                override suspend fun foo(msg: String): String {
//                    return StringUtils.capitalize(msg)
//                }
//
//                override suspend fun proxy(request: ServerHttpRequest): ResponseEntity<String> {
//                    throw UnsupportedOperationException()
//                }
//            }
//        }
//    }
//}
