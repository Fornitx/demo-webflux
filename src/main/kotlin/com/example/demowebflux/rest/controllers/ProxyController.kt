package com.example.demowebflux.rest.controllers

import com.example.demowebflux.constants.CONTEXT_REQUEST_ID
import com.example.demowebflux.constants.CONTEXT_USER_ID
import com.example.demowebflux.constants.PATH_API_V2_PROXY
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.rest.service.DemoService
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.coroutines.coroutineContext

@Tag(name = "proxy-controller", description = "Proxifying API")
@RestController
@RequestMapping(PATH_API_V2_PROXY)
class ProxyController(private val service: DemoService, private val metrics: DemoMetrics) {
    @RequestMapping("/{category}/**")
    suspend fun call(
        @PathVariable category: String,
        request: ServerHttpRequest,
    ): ResponseEntity<String> {
        val context = coroutineContext[ReactorContext]!!.context
        val requestId = context.get<String>(CONTEXT_REQUEST_ID)
        val userId = context.get<String>(CONTEXT_USER_ID)
        return metrics.withHttpTimings(requestId, userId, request.path.value()) {
            service.proxy(request)
        }
    }
}
