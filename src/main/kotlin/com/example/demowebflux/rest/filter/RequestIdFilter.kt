package com.example.demowebflux.rest.filter

import com.example.demowebflux.utils.Constants
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
        val requestId = exchange.request.headers.getFirst(Constants.HEADER_X_REQUEST_ID)
        if (requestId != null) {
            exchange.response.beforeCommit {
                exchange.response.headers.put(Constants.HEADER_X_REQUEST_ID, listOf(requestId))
                Mono.empty()
            }
        }
        return chain.filter(exchange)
    }
}
