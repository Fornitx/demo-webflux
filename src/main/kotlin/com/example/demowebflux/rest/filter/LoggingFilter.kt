package com.example.demowebflux.rest.filter

import com.example.demowebflux.constants.ATTRIBUTE_REQUEST_WAS_LOGGED
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.constants.PATH_V1
import org.reactivestreams.Publisher
import org.springframework.core.annotation.Order
import org.springframework.core.io.buffer.DataBuffer
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
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels

@Component
@Order(2)
class LoggingFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        if (!request.uri.path.startsWith(PATH_V1)) {
            return chain.filter(exchange)
        }
        val requestId = request.headers.getFirst(HEADER_X_REQUEST_ID)
        val relativePath = request.uri

        if (!RequestLogger.log.isDebugEnabled()) {
            exchange.attributes[ATTRIBUTE_REQUEST_WAS_LOGGED] = true
            RequestLogger.logRequest(requestId, relativePath.path)
        }

        val result = chain.filter(LoggingWebExchange(exchange))
        if (!RequestLogger.log.isDebugEnabled()) {
            exchange.response.beforeCommit {
                Mono.fromRunnable {
                    RequestLogger.logResponse(requestId, relativePath.path, exchange.response.statusCode?.value())
                }
            }
        }
        return result
    }

    class LoggingWebExchange(delegate: ServerWebExchange) : ServerWebExchangeDecorator(delegate) {
        private val requestDecorator: LoggingRequestDecorator = LoggingRequestDecorator(delegate)
        private val responseDecorator: LoggingResponseDecorator = LoggingResponseDecorator(delegate)

        override fun getRequest(): ServerHttpRequest = requestDecorator
        override fun getResponse(): ServerHttpResponse = responseDecorator
    }

    class LoggingRequestDecorator(exchange: ServerWebExchange) : ServerHttpRequestDecorator(exchange.request) {
        private val body: Flux<DataBuffer>

        override fun getBody(): Flux<DataBuffer> = body

        init {
            body = if (RequestLogger.log.isDebugEnabled()) {
                super.getBody().doOnNext { dataBuffer ->
                    val bodyStream = ByteArrayOutputStream()
                    val channel = Channels.newChannel(bodyStream)
                    dataBuffer.readableByteBuffers().forEach(channel::write)

                    val request = exchange.request
                    val requestId = request.headers.getFirst(HEADER_X_REQUEST_ID)
                    val relativePath = request.uri

                    exchange.attributes[ATTRIBUTE_REQUEST_WAS_LOGGED] = true
                    RequestLogger.logRequest(requestId, relativePath.path, String(bodyStream.toByteArray()))
                }
            } else {
                super.getBody()
            }
        }
    }

    class LoggingResponseDecorator(
        private val exchange: ServerWebExchange
    ) : ServerHttpResponseDecorator(exchange.response) {
        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> = if (RequestLogger.log.isDebugEnabled()) {
            super.writeWith(Flux.from(body)
                .doOnNext { dataBuffer ->
                    val bodyStream = ByteArrayOutputStream()
                    val channel = Channels.newChannel(bodyStream)
                    dataBuffer.readableByteBuffers().forEach(channel::write)

                    val request = exchange.request
                    val requestId = request.headers.getFirst(HEADER_X_REQUEST_ID)
                    val relativePath = request.uri

                    RequestLogger.logResponse(
                        requestId,
                        relativePath.path,
                        delegate.statusCode?.value(),
                        String(bodyStream.toByteArray())
                    )
                })
        } else {
            super.writeWith(body)
        }
    }
}
