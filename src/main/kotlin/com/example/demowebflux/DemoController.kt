package com.example.demowebflux

import com.example.demowebflux.data.DemoRequest
import com.example.demowebflux.data.DemoResponse
import com.example.demowebflux.filters.TraceIdFilter.Companion.TRACE_ID_HEADER
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import javax.validation.Valid

private val log = KotlinLogging.logger {}

@RestController
class DemoController(private val service: DemoService) {
    companion object {
        const val FOO_PATH = "/foo"
    }

    @PostMapping(
        FOO_PATH,
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun foo(
        @RequestHeader(TRACE_ID_HEADER) traceId: String,
        @RequestBody @Valid request: DemoRequest,
    ): Mono<DemoResponse> {
        return service.foo(request.msg).map(::DemoResponse)
    }
}
