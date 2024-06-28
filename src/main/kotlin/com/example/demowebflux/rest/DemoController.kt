package com.example.demowebflux.rest

import com.example.demowebflux.constants.API
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.constants.LOGSTASH_REQUEST_ID
import com.example.demowebflux.constants.V1
import com.example.demowebflux.constants.V2
import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import io.github.oshai.kotlinlogging.coroutines.withLoggingContextAsync
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "proxy-controller", description = "Proxifying API")
@RestController
@RequestMapping(API)
class DemoController(private val service: DemoService) {
    @Operation(summary = "Calling /foo summary", description = "Calling /foo description")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "OK"),
        ApiResponse(responseCode = "404", description = "Not Found"),
    )
    @PostMapping(
        "$V1/foo/{fooId}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    suspend fun foo(
        @PathVariable fooId: String,
        @Parameter(description = "Foo New ID", required = true)
        @RequestParam fooNewId: String,
        @RequestHeader(HEADER_X_REQUEST_ID) requestId: String,
        @RequestBody @Valid request: DemoRequest,
    ): ResponseEntity<DemoResponse> {
        return withLoggingContextAsync(LOGSTASH_REQUEST_ID to requestId) {
            service.foo(request)
        }
    }

    @RequestMapping("$V2/**")
    suspend fun call(
        @RequestHeader(HEADER_X_REQUEST_ID) requestId: String,
        request: ServerHttpRequest,
    ): ResponseEntity<String> {
//        val context = coroutineContext[ReactorContext]!!.context
//        val requestId = context.get<String>(CONTEXT_REQUEST_ID)
//        val userId = context.get<String>(CONTEXT_USER_ID)
        return withLoggingContextAsync(LOGSTASH_REQUEST_ID to requestId) {
            service.proxy(request)
        }
    }
}
