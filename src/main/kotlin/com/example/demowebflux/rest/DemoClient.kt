package com.example.demowebflux.rest

import com.example.demowebflux.constants.ATTRIBUTE_REQUEST_ID
import com.example.demowebflux.constants.LOGSTASH_REQUEST_ID
import com.example.demowebflux.constants.PREFIX
import com.example.demowebflux.logging.ClientHttpLogger
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.properties.DemoProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.slf4j.MDCContext
import org.springframework.http.ResponseEntity
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.CoExchangeFilterFunction
import org.springframework.web.reactive.function.client.CoExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.util.UriComponentsBuilder
import reactor.netty.channel.MicrometerChannelMetricsRecorder
import reactor.netty.http.client.HttpClient
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.coroutines.coroutineContext

private val log = KotlinLogging.logger {}

class DemoClient(
    webClientBuilder: WebClient.Builder,
    private val properties: DemoProperties.ClientProperties,
    private val metrics: DemoMetrics,
) {
    private val webClient: WebClient

    init {
        val httpClient = HttpClient.create()
            .responseTimeout(properties.responseTimeout)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectionTimeout.toMillis().toInt())
            .doOnConnected { conn ->
                conn.addHandler(ReadTimeoutHandler(properties.readTimeout.toMillis(), TimeUnit.MILLISECONDS))
                conn.addHandler(WriteTimeoutHandler(properties.writeTimeout.toMillis(), TimeUnit.MILLISECONDS))
            }
            .metrics(
                properties.enableMetrics,
                Supplier { MicrometerChannelMetricsRecorder("${PREFIX}_democlient", "") }
            )
//            .wiretap("reactor.netty.http.client.HttpClient", LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL)
        webClient = webClientBuilder.baseUrl(properties.url)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .filters {
                it.add(logRequest())
                it.add(logResponse())
            }
            .build()
    }

    suspend fun foo(msg: String): String {
        return webClient.post()
            .bodyValue(msg)
            .awaitExchange { it.awaitEntity<String>() }
            .body!!
    }

    suspend fun proxy(path: String, request: ServerHttpRequest): ResponseEntity<String> {
        val metricsUri = UriComponentsBuilder.fromHttpUrl(properties.url).path(path).build().toUriString()
        return metrics.httpClientCallWithMetrics(request.method, metricsUri) {
            proxyInternal(path, request)
        }
    }

    suspend fun proxyInternal(oaht: String, request: ServerHttpRequest): ResponseEntity<String> {
        val requestId = coroutineContext[MDCContext.Key]!!.contextMap!![LOGSTASH_REQUEST_ID]!!
        return webClient.method(request.method)
            .uri { uriBuilder ->
                uriBuilder.path(oaht)
                    .queryParams(request.queryParams)
                    .build()
            }
            .headers { headers -> headers.addAll(request.headers) }
            // TODO cookies
            .body(BodyInserters.fromDataBuffers(request.body))
            // https://github.com/spring-projects/spring-framework/issues/32148
            .context { ctx -> ctx.put(ATTRIBUTE_REQUEST_ID, requestId) }
            .awaitExchange { it.awaitEntity<String>() }
    }

    private fun logRequest(): CoExchangeFilterFunction = object : CoExchangeFilterFunction() {
        override suspend fun filter(request: ClientRequest, next: CoExchangeFunction): ClientResponse {
            ClientHttpLogger.logRequest(request)
            return next.exchange(request)
        }
    }

    private fun logResponse(): CoExchangeFilterFunction = object : CoExchangeFilterFunction() {
        override suspend fun filter(request: ClientRequest, next: CoExchangeFunction): ClientResponse {
            val response = next.exchange(request)
            // https://github.com/spring-projects/spring-framework/issues/32148
            withLoggingContext(
                LOGSTASH_REQUEST_ID to coroutineContext[ReactorContext]!!.context[ATTRIBUTE_REQUEST_ID]
            ) {
                ClientHttpLogger.logResponse(response)
            }
            return response
        }
    }
}
