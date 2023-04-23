package com.example.demowebflux.rest.filter

import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.constants.LOGSTASH_RELATIVE_PATH
import com.example.demowebflux.constants.LOGSTASH_REQUEST_ID
import com.example.demowebflux.data.DemoErrorResponse
import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.errors.DemoRestException
import com.example.demowebflux.metrics.DemoMetrics
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import io.github.oshai.KotlinLogging
import io.github.oshai.withLoggingContext
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponse
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.*

private val log = KotlinLogging.logger {}

@Component
@Order(-2)
class GlobalErrorWebExceptionHandler(
    errorAttributes: ErrorAttributes,
    webProperties: WebProperties,
    serverProperties: ServerProperties,
    applicationContext: ApplicationContext,
    configurer: ServerCodecConfigurer,
    private val objectMapper: ObjectMapper,
    private val metrics: DemoMetrics,
) : DefaultErrorWebExceptionHandler(
    errorAttributes, webProperties.resources, serverProperties.error, applicationContext
) {
    init {
        setMessageReaders(configurer.readers)
        setMessageWriters(configurer.writers)
    }

    private fun getStatusAndDemoError(error: Throwable?): Pair<Int, DemoError> {
        if (error is DemoRestException) {
            return error.demoError.toPair()
        }
        if (error is ErrorResponse) {
            if (error.statusCode.is4xxClientError) {
                return error.statusCode.value() to DemoError.UNEXPECTED_4XX_ERROR
            } else if (error.statusCode.is5xxServerError) {
                return error.statusCode.value() to DemoError.UNEXPECTED_5XX_ERROR
            }
        }
        return DemoError.UNEXPECTED_5XX_ERROR.toPair()
    }

    private fun getErrorAndResponse(request: ServerRequest): Pair<DemoError, DemoErrorResponse> {
        val error = getError(request)
        val (httpStatus, demoError) = getStatusAndDemoError(error)
        val errorResponse = DemoErrorResponse(
            OffsetDateTime.now(),
            request.path(),
            request.headers().firstHeader(HEADER_X_REQUEST_ID),
            httpStatus,
            demoError.code,
            demoError.message,
            error.message,
        )
        return demoError to errorResponse
    }

    override fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
        val error = getError(request)
        val (demoError, errorResponse) = getErrorAndResponse(request)
        val errorAttributes = objectMapper.convertValue<MutableMap<String, Any?>>(errorResponse)
        val httpStatus = errorResponse.status

        metrics.error(demoError).increment()

        withLoggingContext(buildMap {
            errorResponse.requestId?.also {
                put(LOGSTASH_REQUEST_ID, it)
            }
            put(LOGSTASH_RELATIVE_PATH, errorResponse.path)
        }) {
            if (log.isDebugEnabled) {
                log.error(
                    "Response [httpStatus={}, errorCode={}, body={}]",
                    httpStatus,
                    demoError.code,
                    objectMapper.writeValueAsString(errorAttributes),
                    error
                )
            } else {
                log.error("Response [httpStatus={}, errorCode={}]", httpStatus, demoError.code, error)
            }
        }

        return ServerResponse.status(httpStatus)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(errorAttributes))
    }

    override fun logError(request: ServerRequest?, response: ServerResponse?, throwable: Throwable?) {
    }
}
