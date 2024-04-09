package com.example.demowebflux.rest.client

import com.example.demowebflux.properties.DemoProperties
import io.netty.channel.ChannelOption
import io.netty.handler.logging.LogLevel
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.http.ResponseEntity
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.logging.AdvancedByteBufFormat
import java.util.concurrent.TimeUnit

class DemoClientImpl(
    webClientBuilder: WebClient.Builder,
    private val properties: DemoProperties.ClientProperties
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
                    .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL)
            )
        )
        .build()

    override suspend fun call(msg: String): String {
        return client.post()
            .bodyValue(msg)
            .awaitExchange<ResponseEntity<String>>(ClientResponse::awaitEntity)
            .body!!
    }
}
