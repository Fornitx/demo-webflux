package com.example.demowebflux.rest.filter

import com.example.demowebflux.constants.CONTEXT_REQUEST_ID
import com.example.demowebflux.constants.HEADER_X_REQUEST_ID
import com.example.demowebflux.constants.PATH_API
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@Order(1)
class RequestIdFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (!exchange.request.path.value().startsWith(PATH_API)) {
            return chain.filter(exchange)
        }

        val requestId = exchange.request.headers.getFirst(HEADER_X_REQUEST_ID)
        if (requestId != null) {
            exchange.response.beforeCommit {
                exchange.response.headers[HEADER_X_REQUEST_ID] = requestId
                Mono.empty()
            }
            return chain.filter(exchange).contextWrite { ctx ->
                ctx.put(CONTEXT_REQUEST_ID, requestId)
            }
        }
        return chain.filter(exchange)
    }
}
