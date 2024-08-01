package com.example.demowebflux.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.blockhound.BlockingOperationError
import reactor.core.publisher.Mono
import java.time.Duration
import kotlin.test.assertFails

private val log = KotlinLogging.logger { }

class BlockHoundTest {
    @Test
    fun test() {
        val throwable = assertFails {
            Mono.delay(Duration.ofMillis(100))
                .doOnNext { Thread.sleep(100) }
                .block()
        }
        log.error(throwable) { }
        assertThat(throwable.cause).isInstanceOf(BlockingOperationError::class.java)
    }
}
