package com.example.demowebflux.filters

import com.example.demowebflux.DemoController
import com.example.demowebflux.data.DemoErrorResponse
import com.example.demowebflux.error.PredictableException
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.web.ServerProperties
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger {}

@Component
@Order(-2) // override DefaultErrorWebExceptionHandler
class GlobalErrorWebExceptionHandler(
    private val objectMapper: ObjectMapper,
    errorAttributes: ErrorAttributes,
    webProperties: WebProperties,
    serverProperties: ServerProperties,
    applicationContext: ApplicationContext,
    serverCodecConfigurer: ServerCodecConfigurer,
) : DefaultErrorWebExceptionHandler(
    errorAttributes,
    webProperties.resources,
    serverProperties.error,
    applicationContext,
) {
    init {
        setMessageReaders(serverCodecConfigurer.readers)
        setMessageWriters(serverCodecConfigurer.writers)
    }

    override fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
        if (request.path() == DemoController.FOO_PATH) {
            val error = getError(request)
            val errorAttributes = getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.ALL))

            val status = if (error is PredictableException) HttpStatus.OK.value() else getHttpStatus(errorAttributes)
            val code = if (error is PredictableException) PredictableException.STATUS else status

            // TODO logging response body
            val response = DemoErrorResponse(code, error.message!!)
            if (log.isDebugEnabled) {
                log.debug(
                    "Response [{}] {}\n{}",
                    request.exchange().request.id,
                    status,
                    objectMapper.writeValueAsString(response)
                )
            } else {
                log.info("Response [{}] {}", request.exchange().request.id, status)
            }
            val serverHttpResponse = request.exchange().response
            return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(response)
        }

        return super.renderErrorResponse(request)
    }

    override fun logError(request: ServerRequest?, response: ServerResponse?, throwable: Throwable?) {
        super.logError(request, response, throwable)
    }
}
