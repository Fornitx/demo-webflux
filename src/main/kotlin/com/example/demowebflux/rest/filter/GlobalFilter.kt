package com.example.demowebflux.rest.filter

import com.example.demowebflux.constants.API
import com.example.demowebflux.constants.ATTRIBUTE_DEMO_TOKEN
import com.example.demowebflux.constants.ATTRIBUTE_REQUEST_ID
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.logging.ServerHttpLogger
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.rest.exceptions.BadRequestException
import com.example.demowebflux.rest.exceptions.ForbiddenException
import com.example.demowebflux.rest.exceptions.UnauthorizedException
import com.example.demowebflux.utils.JwtUtils
import io.micrometer.core.instrument.Timer
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebExchangeDecorator
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.Charset

@Component
class GlobalFilter(private val metrics: DemoMetrics) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        if (!request.path.value().startsWith(API)) {
            return chain.filter(exchange)
        }

        val timer = Timer.start()

        val requestId = exchange.request.headers.getFirst(HEADER_X_REQUEST_ID)

        // log request
        ServerHttpLogger.logRequest(exchange)

        exchange.response.beforeCommit {
            // add requestId header to response
            if (requestId != null) {
                exchange.response.headers[HEADER_X_REQUEST_ID] = requestId
            }
            // log response
            ServerHttpLogger.logResponse(exchange)
            // response metrics
            timer.stop(
                metrics.httpServerResponses(
                    request.method,
                    request.uri.path,
                    exchange.response.statusCode,
                )
            )
            Mono.empty()
        }

        // request metrics
        metrics.httpServerRequests(request.method, request.uri.path).increment()

        // TODO messageSource
//        log.info {
//            messageSource.getMessage("service.proxy", emptyArray(), LocaleContextHolder.getLocale())
//        }

        if (requestId == null) {
            throw BadRequestException("Required header '$HEADER_X_REQUEST_ID' is missing")
        }

        // parse auth header into DemoToken
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            ?: throw UnauthorizedException("Required header '${HttpHeaders.AUTHORIZATION}' is missing")

        val demoToken = try {
            JwtUtils.parseDemoToken(authHeader)
        } catch (ex: Exception) {
            throw UnauthorizedException("Can't parse header '${HttpHeaders.AUTHORIZATION}'", ex)
        }

        // log JWT token
        ServerHttpLogger.logToken(requestId, demoToken)
        // save JWT token to reach it in controller
        exchange.attributes[ATTRIBUTE_DEMO_TOKEN] = demoToken

        if (demoToken.audiences.size < 2) {
            throw ForbiddenException("Audience not found")
        }

        return chain.filter(if (ServerHttpLogger.log.isDebugEnabled()) LoggingWebExchange(exchange) else exchange)
            // save requestId in Reactor context to reach it in ExchangeFilterFunction
            .contextWrite { ctx -> ctx.put(ATTRIBUTE_REQUEST_ID, requestId) }
    }

    class LoggingWebExchange(delegate: ServerWebExchange) : ServerWebExchangeDecorator(delegate) {
        private val requestDecorator: LoggingRequestDecorator = LoggingRequestDecorator(delegate)
        private val responseDecorator: LoggingResponseDecorator = LoggingResponseDecorator(delegate)

        override fun getRequest(): ServerHttpRequest = requestDecorator
        override fun getResponse(): ServerHttpResponse = responseDecorator
    }

    class LoggingRequestDecorator(exchange: ServerWebExchange) : ServerHttpRequestDecorator(exchange.request) {
        private val body: Flux<DataBuffer> = Flux.defer {
            val body = StringBuilder()
            super.getBody().doOnNext { dataBuffer ->
                body.append(dataBuffer.toString(Charset.defaultCharset()))
            }.doOnComplete {
                ServerHttpLogger.logRequestBody(exchange, body.toString())
            }
        }

        override fun getBody(): Flux<DataBuffer> = body
    }

    class LoggingResponseDecorator(
        private val exchange: ServerWebExchange
    ) : ServerHttpResponseDecorator(exchange.response) {
        override fun writeWith(dataBuffers: Publisher<out DataBuffer>): Mono<Void> = Mono.defer {
            val body = StringBuilder()
            super.writeWith(
                Flux.from(dataBuffers)
                    .doOnNext { dataBuffer ->
                        body.append(dataBuffer.toString(Charset.defaultCharset()))
                    }.doOnComplete {
                        ServerHttpLogger.logResponseBody(exchange, body.toString())
                    }
            )
        }
    }
}
