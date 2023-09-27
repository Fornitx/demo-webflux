package com.example.demowebflux.rest

import com.example.demowebflux.constants.CONTEXT_USER_ID
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.constants.PATH_V1
import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.rest.service.DemoService
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.*
import kotlin.coroutines.coroutineContext

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping(PATH_V1)
class DemoController(private val service: DemoService, private val metrics: DemoMetrics) {
    @PostMapping(
        "/foo/{fooId}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun foo(
        @RequestHeader(AUTHORIZATION) authorization: String,
        @RequestHeader(HEADER_X_REQUEST_ID) requestId: UUID,
        @PathVariable fooId: String,
        @RequestBody @Valid request: DemoRequest,
    ): DemoResponse {
        val userId = coroutineContext[ReactorContext]!!.context.get<String>(CONTEXT_USER_ID)
        return metrics.withHttpTimings(requestId, userId, "$PATH_V1/foo/$fooId") {
            service.foo(request)
        }
    }
}
