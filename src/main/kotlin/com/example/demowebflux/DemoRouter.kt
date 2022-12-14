package com.example.demowebflux

import com.example.demowebflux.data.DemoErrorResponse
import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.example.demowebflux.error.ErrorCodes
import com.example.demowebflux.error.PredictableException
import com.example.demowebflux.filters.TraceIdFilter.Companion.TRACE_ID_HEADER
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.onErrorResume
import javax.validation.ConstraintViolationException
import javax.validation.Validator

private val log = KotlinLogging.logger {}

@Configuration
class DemoRouter(private val validator: Validator, private val service: DemoService) {
    companion object {
        const val BAR_PATH = "/bar"
    }

    @Bean
    fun route() = router {
//        accept(MediaType.APPLICATION_JSON).nest {
        POST(BAR_PATH, ::bar)
//        }
    }

    private fun bar(serverRequest: ServerRequest): Mono<ServerResponse> {
        val traceId = serverRequest.headers().firstHeader(TRACE_ID_HEADER)
        if (traceId === null) {
            return ServerResponse.badRequest()
                .bodyValue(DemoErrorResponse(ErrorCodes.COMMON_ERROR, "No '$TRACE_ID_HEADER' header"))
        }
        val contentType = serverRequest.headers().contentType().get()
        if (!MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
            return ServerResponse.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .bodyValue(DemoErrorResponse(ErrorCodes.COMMON_ERROR, "ContentType $contentType is not supported"))
        }
        val acceptList = serverRequest.headers().accept()
        if (acceptList.all { !it.isCompatibleWith(MediaType.APPLICATION_JSON) }) {
            return ServerResponse.status(HttpStatus.NOT_ACCEPTABLE)
                .bodyValue(DemoErrorResponse(ErrorCodes.COMMON_ERROR, "Accepts $acceptList are not supported"))
        }
        return serverRequest.bodyToMono<DemoRequest>()
            .flatMap { request ->
                log.info { "/foo $traceId $request" }

                // validation
                val violations = validator.validate(request)
                if (violations.isNotEmpty()) {
                    return@flatMap ServerResponse.badRequest()
                        .bodyValue(DemoErrorResponse(ErrorCodes.COMMON_ERROR, ConstraintViolationException(violations).message))
                }

                return@flatMap service.bar(request.msg)
                    .flatMap { ServerResponse.ok().bodyValue(DemoResponse(it, _anyField = request._anyField)) }
                    .onErrorResume(PredictableException::class) {
                        ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .bodyValue(DemoErrorResponse(it.errorCode, it.message))
                    }
                    .onErrorResume {
                        ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .bodyValue(DemoErrorResponse(ErrorCodes.COMMON_ERROR, it.message))
                    }
            }
    }
}
