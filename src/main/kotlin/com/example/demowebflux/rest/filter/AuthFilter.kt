package com.example.demowebflux.rest.filter

import com.example.demowebflux.constants.CONTEXT_USER_ID
import com.example.demowebflux.constants.PATH_V1
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@Order(3)
class AuthFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (!exchange.request.uri.path.startsWith(PATH_V1)) {
            return chain.filter(exchange)
        }

        return chain.filter(exchange).contextWrite { ctx ->
            ctx.put(CONTEXT_USER_ID, "John")
        }
    }
}
