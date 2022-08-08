package com.example.demowebflux

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.stream.Collectors
import java.util.stream.Stream

@Service
class DemoService {
    fun foo(msg: String): Mono<String> {
        return Mono.fromCallable { msg.repeat(3) }
    }

    fun bar(msg: String): Mono<String> {
        return Mono.fromCallable {
            Stream.generate(msg::uppercase)
                .limit(3)
                .collect(Collectors.joining("~"))
        }
    }
}
