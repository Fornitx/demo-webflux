package com.example.demowebflux.filters

import mu.KLogging
import org.reactivestreams.Publisher
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
class LoggingWebFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return chain.filter(LoggingWebExchange(exchange))
    }

    class LoggingWebExchange(delegate: ServerWebExchange) : ServerWebExchangeDecorator(delegate) {
        private val requestDecorator = LoggingRequestDecorator(delegate.request)
        private val responseDecorator = LoggingResponseDecorator(delegate.request, delegate.response)

        override fun getRequest() = requestDecorator
        override fun getResponse() = responseDecorator
    }

    class LoggingRequestDecorator(delegate: ServerHttpRequest) : ServerHttpRequestDecorator(delegate) {
        companion object : KLogging()

        private val body: Flux<DataBuffer>?

        override fun getBody(): Flux<DataBuffer> {
            return body!!
        }

        init {
            logger.info(
                "Request [{}] {} {} from {}\n{}",
                delegate.id,
                delegate.methodValue,
                delegate.path,
                delegate.remoteAddress,
                delegate.headers
            )
            body = if (logger.isDebugEnabled) {
                super.getBody().doOnNext { buffer ->
                    val baos = ByteArrayOutputStream()
                    Channels.newChannel(baos).write(buffer.asByteBuffer().asReadOnlyBuffer())
                    logger.debug("Request [{}] bodyPart: {}", delegate.id, baos)
                }
            } else {
                super.getBody()
            }
        }
    }

    class LoggingResponseDecorator(
        private val request: ServerHttpRequest, delegate: ServerHttpResponse
    ) : ServerHttpResponseDecorator(delegate) {
        companion object : KLogging()

        override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
            return if (logger.isDebugEnabled) {
                super.writeWith(
                    Flux.from(body).doOnNext { buffer ->
                        val baos = ByteArrayOutputStream()
                        Channels.newChannel(baos).write(buffer.asByteBuffer().asReadOnlyBuffer())
                        logger.debug("Response [{}] bodyPart: {}", request.id, baos)
                    })
            } else {
                super.writeWith(body)
            }
        }

        init {
            delegate.beforeCommit {
                // TODO LOG ORDER (bodyPart, then status)
                Mono.fromRunnable {
                    logger.info("Response [{}] {}\n{}", request.id, delegate.statusCode, delegate.headers)
                }
            }
        }
    }
}
