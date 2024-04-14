package com.example.demowebflux.rest.service

import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.errors.DemoRestException
import com.example.demowebflux.properties.DemoProperties.ServiceProperties
import com.example.demowebflux.rest.client.DemoClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest

private val log = KotlinLogging.logger {}

class DemoServiceImpl(
    private val properties: ServiceProperties,
    private val client: DemoClient,
    private val messageSource: MessageSource,
) : DemoService {
    override suspend fun foo(request: DemoRequest): ResponseEntity<DemoResponse> {
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

    override suspend fun proxy(request: ServerHttpRequest): ResponseEntity<String> {
        log.info {
            messageSource.getMessage("service.proxy", emptyArray(), LocaleContextHolder.getLocale())
        }
        return client.proxy(request)
    }
}
