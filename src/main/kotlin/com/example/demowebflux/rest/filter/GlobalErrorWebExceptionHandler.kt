package com.example.demowebflux.rest.filter

import com.example.demowebflux.data.DemoErrorResponse
import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.errors.DemoRestException
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.utils.Constants
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.KotlinLogging
import io.github.oshai.withLoggingContext
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponse
import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger {}

@Component
@Order(-2)
class GlobalErrorWebExceptionHandler(
    errorAttributes: ErrorAttributes,
    webProperties: WebProperties,
    applicationContext: ApplicationContext,
    configurer: ServerCodecConfigurer,
    private val objectMapper: ObjectMapper,
    private val metrics: DemoMetrics,
) : AbstractErrorWebExceptionHandler(
    errorAttributes, webProperties.resources, applicationContext
) {
    init {
        setMessageWriters(configurer.writers)
    }

    override fun getRoutingFunction(errorAttributes: ErrorAttributes?): RouterFunction<ServerResponse> {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse)
    }

    private fun renderErrorResponse(serverRequest: ServerRequest): Mono<ServerResponse> {
        val headers = serverRequest.headers()
        val requestId = headers.firstHeader(Constants.HEADER_X_REQUEST_ID)

        val path = serverRequest.path()

        val error = getError(serverRequest)
        val demoError = handleError(error)
        metrics.error(demoError).increment()

        val bodyValue = DemoErrorResponse(demoError, error)
        withLoggingContext(
            Constants.LOGSTASH_REQUEST_ID to requestId,
            Constants.LOGSTASH_RELATIVE_PATH to path
        ) {
            if (log.isDebugEnabled) {
                log.error(
                    "Response [httpStatus={}, errorCode={}, body={}]",
                    demoError.httpStatus,
                    demoError.code,
                    objectMapper.writeValueAsString(bodyValue),
                    error
                )
            } else {
                log.error(
                    "Response [httpStatus={}, errorCode={}]",
                    demoError.httpStatus,
                    demoError.code,
                    error
                )
            }
        }
        return ServerResponse.status(demoError.httpStatus)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(bodyValue)
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
}
