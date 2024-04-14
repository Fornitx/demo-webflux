package com.example.demowebflux.rest.controllers

import com.example.demowebflux.constants.CONTEXT_REQUEST_ID
import com.example.demowebflux.constants.CONTEXT_USER_ID
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.constants.PATH_API_V1
import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.example.demowebflux.metrics.DemoMetrics
import com.example.demowebflux.rest.service.DemoService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import kotlin.coroutines.coroutineContext

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping(PATH_API_V1)
class DemoController(private val service: DemoService, private val metrics: DemoMetrics) {
    @Operation(summary = "Calling /foo summary", description = "Calling /foo description")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "OK"),
        ApiResponse(responseCode = "404", description = "Not Found"),
    )
    @PostMapping(
        "/foo/{fooId}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun call(
        @PathVariable fooId: String,
        @Parameter(description = "Foo New ID", required = true)
        @RequestParam fooNewId: String,
        @RequestHeader(HEADER_X_REQUEST_ID) requestId: String,
        @RequestBody @Valid request: DemoRequest
    ): ResponseEntity<DemoResponse> {
        val context = coroutineContext[ReactorContext]!!.context
        val requestId = context.get<String>(CONTEXT_REQUEST_ID)
        val userId = context.get<String>(CONTEXT_USER_ID)

        log.info { "Foo id: $fooId => $fooNewId" }

        return metrics.withHttpTimings(requestId, userId, "$PATH_API_V1/foo/$fooId") {
            service.foo(request)
        }
    }
}
