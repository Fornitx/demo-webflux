package com.example.demowebflux.logging

import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.constants.LOGSTASH_REQUEST_ID
import com.example.demowebflux.utils.DemoToken
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import org.springframework.http.HttpHeaders
import org.springframework.web.server.ServerWebExchange

object ServerHttpLogger {
    val log = KotlinLogging.logger { }

    fun logRequest(exchange: ServerWebExchange) {
        val request = exchange.request
        val method = request.method.name()
        val uri = request.uri.toString()
        val requestId = request.headers.getFirst(HEADER_X_REQUEST_ID)

        withLoggingContext(LOGSTASH_REQUEST_ID to requestId) {
            if (log.isDebugEnabled()) {
                val headers = request.headers.listHeaders()
                log.debug("Request {} {}\n{}", method, uri, headers)
            } else {
                log.info("Request {} {}", method, uri)
            }
        }
    }

    fun logResponse(exchange: ServerWebExchange) {
        val requestId = exchange.request.headers.getFirst(HEADER_X_REQUEST_ID)
        val response = exchange.response
        val httpStatus = response.statusCode?.value().toString()

        withLoggingContext(LOGSTASH_REQUEST_ID to requestId) {
            if (log.isDebugEnabled()) {
                val headers = response.headers.listHeaders()
                log.debug("Response {}\n{}", httpStatus, headers)
            } else {
                log.info("Response {}", httpStatus)
            }
        }
    }

    fun logRequestBody(exchange: ServerWebExchange, body: String?) {
        if (body.isNullOrEmpty()) {
            return
        }
        if (log.isDebugEnabled()) {
            val requestId = exchange.request.headers.getFirst(HEADER_X_REQUEST_ID)
            withLoggingContext(LOGSTASH_REQUEST_ID to requestId) {
                log.debug("Request body:\n{}", body)
            }
        }
    }

    fun logResponseBody(exchange: ServerWebExchange, body: String?) {
        if (body.isNullOrEmpty()) {
            return
        }
        if (log.isDebugEnabled()) {
            val requestId = exchange.request.headers.getFirst(HEADER_X_REQUEST_ID)
            withLoggingContext(LOGSTASH_REQUEST_ID to requestId) {
                log.debug("Response body:\n{}", body)
            }
        }
    }

    fun logErrorResponse(
        requestId: String?,
        relativePath: String?,
        httpStatus: Int,
        errorCode: Int,
        error: Throwable,
        body: String? = null
    ) {
        withLoggingContext(
            LOGSTASH_REQUEST_ID to requestId,
        ) {
            if (log.isDebugEnabled()) {
                log.error(error) { "Response [httpStatus=$httpStatus, errorCode=$errorCode, body=$body]" }
            } else {
                log.error(error) { "Response [httpStatus=$httpStatus, errorCode=$errorCode]" }
            }
        }
    }

    fun logToken(requestId: String, demoToken: DemoToken) = withLoggingContext(
        LOGSTASH_REQUEST_ID to requestId
    ) {
        if (log.isDebugEnabled()) {
            log.debug("JWT token: {}", demoToken)
        } else {
            log.info("JWT token aud: {}", demoToken.audiences)
        }
    }

    private fun HttpHeaders.listHeaders(): String =
        this.map { "${it.key} : ${it.value}" }.joinToString(System.lineSeparator())
}
