package com.example.demowebflux.filters

import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class TraceIdFilter : WebFilter {
    companion object {
        const val TRACE_ID_HEADER = "trace-id"
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        exchange.request.headers.getFirst(TRACE_ID_HEADER)?.also {
            exchange.response.headers.set(TRACE_ID_HEADER, it)
        }

        return chain.filter(exchange)
    }
}