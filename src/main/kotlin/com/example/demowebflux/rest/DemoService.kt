package com.example.demowebflux.rest

import com.example.demowebflux.constants.API_V2
import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.errors.DemoRestException
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.properties.DemoProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest

private val log = KotlinLogging.logger {}

class DemoService(
    private val properties: DemoProperties.ServiceProperties,
    private val client: DemoClient,
    private val messageSource: MessageSource,
    private val metrics: DemoMetrics,
) {
    suspend fun foo(request: DemoRequest): ResponseEntity<DemoResponse> {
        log.info {
            messageSource.getMessage(
                "service.foo",
                arrayOf(request.nullOrNotBlankStr),
                LocaleContextHolder.getLocale()
            )
        }
        if (request.nullOrNotBlankStr == "0") {
            return ResponseEntity.notFound().build()
        }
        if (request.nullOrNotBlankStr == "666") {
            throw DemoRestException(DemoError.MSG_IS_666)
        }
        return ResponseEntity.ok(
            DemoResponse(
                client.foo(request.nullOrNotBlankStr ?: "").repeat(properties.multiplier),
                others = request.others
            )
        )
    }

    suspend fun proxy(request: ServerHttpRequest): ResponseEntity<String> {
        val path = request.path.value().removePrefix(API_V2)
        return client.proxy(path, request)
    }
}
