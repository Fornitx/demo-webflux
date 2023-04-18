package com.example.demowebflux.rest.filter

import com.example.demowebflux.utils.Constants
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@Order(2)
class AuthFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (!exchange.request.uri.path.startsWith(Constants.PATH_V1)) {
            return chain.filter(exchange)
        }

        return chain.filter(exchange).contextWrite { ctx ->
            ctx.put(Constants.CONTEXT_USER_ID, "John")
        }
    }
}
