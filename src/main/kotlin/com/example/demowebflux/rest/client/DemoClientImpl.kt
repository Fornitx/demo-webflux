package com.example.demowebflux.rest.client

import com.example.demowebflux.properties.DemoProperties.ClientProperties
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.http.ResponseEntity
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange
import reactor.netty.http.client.HttpClient
import java.util.concurrent.TimeUnit

class DemoClientImpl(
    webClientBuilder: WebClient.Builder,
    private val properties: ClientProperties
) : DemoClient {
    private val client = webClientBuilder.baseUrl(properties.url)
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .responseTimeout(properties.responseTimeout)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectionTimeout.toMillis().toInt())
                    .doOnConnected { conn ->
                        conn.addHandler(ReadTimeoutHandler(properties.readTimeout.toMillis(), TimeUnit.MILLISECONDS))
                        conn.addHandler(WriteTimeoutHandler(properties.writeTimeout.toMillis(), TimeUnit.MILLISECONDS))
                    }
//                    .wiretap("reactor.netty.http.client.HttpClient", LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL)
            )
        )
        .build()

    override suspend fun foo(msg: String): String {
        return client.post()
            .bodyValue(msg)
            .awaitExchange { it.awaitEntity<String>() }
            .body!!
    }

    override suspend fun proxy(request: ServerHttpRequest): ResponseEntity<String> {
        return client.method(request.method)
            .uri { uriBuilder -> uriBuilder.path(request.path.value()).queryParams(request.queryParams).build() }
            .headers { headers -> headers.addAll(request.headers) }
            // TODO cookies
            .body(BodyInserters.fromDataBuffers(request.body))
            .awaitExchange { it.awaitEntity<String>() }
    }
}
