package com.example.demowebflux.rest.filter

import com.example.demowebflux.data.DemoErrorResponse
import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.errors.DemoRestException
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.utils.Constants
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

    private fun handleError(error: Throwable?): DemoError {
        if (error is DemoRestException) {
            return error.demoError
        }
        if (error is ErrorResponse) {
            if (error.statusCode.is4xxClientError) {
                return DemoError.UNEXPECTED_400_ERROR
            }
        }
        return DemoError.UNEXPECTED_500_ERROR
    }

    private fun getErrorAndAttributes(request: ServerRequest): Pair<DemoError, MutableMap<String, Any?>> {
        val error = getError(request)
        val demoError = handleError(error)
        val errorResponse = DemoErrorResponse(
            OffsetDateTime.now(),
            request.path(),
            request.headers().firstHeader(Constants.HEADER_X_REQUEST_ID),
            demoError.code,
            demoError.message,
            error.message
        )
        val errorAttributes = objectMapper.convertValue<MutableMap<String, Any?>>(errorResponse)
        return demoError to errorAttributes
    }

    override fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
        val error = getError(request)
        val (demoError, errorAttributes) = getErrorAndAttributes(request)
        val httpStatus = demoError.httpStatus

        metrics.error(demoError).increment()

        withLoggingContext(buildMap {
            errorAttributes["requestId"]?.also {
                put(Constants.LOGSTASH_REQUEST_ID, it.toString())
            }
            errorAttributes["path"]?.also {
                put(Constants.LOGSTASH_RELATIVE_PATH, it.toString())
            }
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
