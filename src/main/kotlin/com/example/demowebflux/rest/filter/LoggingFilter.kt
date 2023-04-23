package com.example.demowebflux.rest.filter

import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.constants.LOGSTASH_RELATIVE_PATH
import com.example.demowebflux.constants.LOGSTASH_REQUEST_ID
import com.example.demowebflux.constants.PATH_V1
import io.github.oshai.KotlinLogging
import io.github.oshai.withLoggingContext
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

private val log = KotlinLogging.logger {}

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

        if (!log.isDebugEnabled) {
            withLoggingContext(
                LOGSTASH_REQUEST_ID to requestId,
                LOGSTASH_RELATIVE_PATH to relativePath.path,
            ) {
                log.info("Request")
            }
        }

        val result = chain.filter(LoggingWebExchange(exchange))
        if (!log.isDebugEnabled) {
            exchange.response.beforeCommit {
                Mono.fromRunnable {
                    withLoggingContext(
                        LOGSTASH_REQUEST_ID to requestId,
                        LOGSTASH_RELATIVE_PATH to relativePath.path,
                    ) {
                        log.info("Response [code={}]", exchange.response.statusCode)
                    }
                }
            }
        }
        return result
    }

    class LoggingWebExchange(delegate: ServerWebExchange) : ServerWebExchangeDecorator(delegate) {
        private val requestDecorator: LoggingRequestDecorator = LoggingRequestDecorator(delegate.request)
        private val responseDecorator: LoggingResponseDecorator =
            LoggingResponseDecorator(delegate.request, delegate.response)

        override fun getRequest(): ServerHttpRequest = requestDecorator
        override fun getResponse(): ServerHttpResponse = responseDecorator
    }

    class LoggingRequestDecorator(delegate: ServerHttpRequest) : ServerHttpRequestDecorator(delegate) {
        private val body: Flux<DataBuffer>

        override fun getBody(): Flux<DataBuffer> {
            return body
        }

        init {
            body = if (log.isDebugEnabled) {
                super.getBody().doOnNext { dataBuffer ->
                    val bodyStream = ByteArrayOutputStream()
                    val channel = Channels.newChannel(bodyStream)
                    dataBuffer.readableByteBuffers().forEach(channel::write)

                    val requestId = delegate.headers.getFirst(HEADER_X_REQUEST_ID)
                    val relativePath = delegate.uri

                    withLoggingContext(
                        LOGSTASH_REQUEST_ID to requestId,
                        LOGSTASH_RELATIVE_PATH to relativePath.path,
                    ) {
                        log.info("Request [body={}]", String(bodyStream.toByteArray()))
                    }
                }
            } else {
                super.getBody()
            }
        }
    }

    class LoggingResponseDecorator(
        private val request: ServerHttpRequest,
        delegate: ServerHttpResponse
    ) : ServerHttpResponseDecorator(delegate) {
        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> = if (log.isDebugEnabled) {
            super.writeWith(Flux.from(body)
                .doOnNext { dataBuffer ->
                    val bodyStream = ByteArrayOutputStream()
                    val channel = Channels.newChannel(bodyStream)
                    dataBuffer.readableByteBuffers().forEach(channel::write)

                    val requestId = request.headers.getFirst(HEADER_X_REQUEST_ID)
                    val relativePath = request.uri

                    withLoggingContext(
                        LOGSTASH_REQUEST_ID to requestId,
                        LOGSTASH_RELATIVE_PATH to relativePath.path,
                    ) {
                        log.debug("Response [body={}]", String(bodyStream.toByteArray()))
                    }
                })
        } else {
            super.writeWith(body)
        }
    }
}
