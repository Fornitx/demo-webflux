package com.example.demowebflux.rest.filter

import com.example.demowebflux.utils.Constants
import io.github.oshai.KLogger
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
        if (!request.uri.path.startsWith(Constants.PATH_V1)) {
            return chain.filter(exchange)
        }
        val requestId = request.headers.getFirst(Constants.HEADER_X_REQUEST_ID)
        val relativePath = request.uri

        if (!log.isDebugEnabled) {
            withLoggingContext(
                Constants.LOGSTASH_REQUEST_ID to requestId,
                Constants.LOGSTASH_RELATIVE_PATH to relativePath.path,
            ) {
                log.info("Request")
            }
        }

        val result = chain.filter(LoggingWebExchange(log, exchange))
        if (!log.isDebugEnabled) {
            exchange.response.beforeCommit {
                Mono.fromRunnable {
                    withLoggingContext(
                        Constants.LOGSTASH_REQUEST_ID to requestId,
                        Constants.LOGSTASH_RELATIVE_PATH to relativePath.path,
                    ) {
                        log.info("Response [code={}]", exchange.response.statusCode)
                    }
                }
            }
        }
        return result
    }

    class LoggingWebExchange(log: KLogger, delegate: ServerWebExchange) : ServerWebExchangeDecorator(delegate) {
        private val requestDecorator: LoggingRequestDecorator = LoggingRequestDecorator(log, delegate.request)
        private val responseDecorator: LoggingResponseDecorator =
            LoggingResponseDecorator(log, delegate.request, delegate.response)

        override fun getRequest(): ServerHttpRequest = requestDecorator
        override fun getResponse(): ServerHttpResponse = responseDecorator
    }

    class LoggingRequestDecorator(log: KLogger, delegate: ServerHttpRequest) :
        ServerHttpRequestDecorator(delegate) {

        private val body: Flux<DataBuffer>?

        override fun getBody(): Flux<DataBuffer> {
            return body!!
        }

        init {
            if (log.isDebugEnabled) {
                body = super.getBody().doOnNext { buffer: DataBuffer ->
                    val bodyStream = ByteArrayOutputStream()
                    // TODO Deprecated
                    Channels.newChannel(bodyStream).write(buffer.asByteBuffer().asReadOnlyBuffer())

                    val requestId = delegate.headers.getFirst(Constants.HEADER_X_REQUEST_ID)
                    val relativePath = delegate.uri

                    withLoggingContext(
                        Constants.LOGSTASH_REQUEST_ID to requestId,
                        Constants.LOGSTASH_RELATIVE_PATH to relativePath.path,
                    ) {
                        log.info("Request [body={}]", String(bodyStream.toByteArray()))
                    }
                }
            } else {
                body = super.getBody()
            }
        }
    }

    class LoggingResponseDecorator(
        private val log: KLogger,
        private val request: ServerHttpRequest,
        delegate: ServerHttpResponse
    ) :
        ServerHttpResponseDecorator(delegate) {
        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
            return if (log.isDebugEnabled) {
                super.writeWith(Flux.from(body)
                    .doOnNext { buffer: DataBuffer ->
                        if (log.isDebugEnabled) {
                            val bodyStream = ByteArrayOutputStream()
                            // TODO Deprecated
                            Channels.newChannel(bodyStream).write(buffer.asByteBuffer().asReadOnlyBuffer())

                            val requestId = request.headers.getFirst(Constants.HEADER_X_REQUEST_ID)
                            val relativePath = request.uri

                            withLoggingContext(
                                Constants.LOGSTASH_REQUEST_ID to requestId,
                                Constants.LOGSTASH_RELATIVE_PATH to relativePath.path,
                            ) {
                                log.debug("Response [body={}]", String(bodyStream.toByteArray()))
                            }
                        }
                    })
            } else {
                super.writeWith(body)
            }
        }
    }
}
