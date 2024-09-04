package com.example.demowebflux

import com.example.demowebflux.utils.withMDCContextAsync
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

private val log = KotlinLogging.logger { }

@TestMethodOrder(MethodOrderer.MethodName::class)
class LoggingTest {
    @Test
    fun `1_testRunBlocking`() {
        runBlocking {
            log.info { "xxx-first" }
            withLoggingContext("logKey" to "logValue") {
                log.info { "xxx-1" }
                withContext(Dispatchers.IO + MDCContext()) {
                    log.info { "xxx-2" }
                }
                log.info { "xxx-3" }
            }
            log.info { "xxx-last" }
        }
    }

    @Test
    fun `2_testGlobalScope`() {
        val job = GlobalScope.launch {
            log.info { "xxx-first" }
            withLoggingContext("logKey" to "logValue") {
                log.info { "xxx-1" }
                withContext(Dispatchers.IO + MDCContext()) {
                    log.info { "xxx-2" }
                }
                log.info { "xxx-3" }
            }
            log.info { "xxx-last" }
        }
        while (!job.isCompleted) {
            Thread.onSpinWait()
        }
    }

    @Test
    fun `3_testGlobalScope_correct`() {
        val job = GlobalScope.launch {
            log.info { "xxx-first" }
            withMDCContextAsync("logKey" to "logValue") {
                log.info { "xxx-1" }
                withContext(Dispatchers.IO + MDCContext()) {
                    log.info { "xxx-2" }
                }
                log.info { "xxx-3" }
            }
            log.info { "xxx-last" }
        }
        while (!job.isCompleted) {
            Thread.onSpinWait()
        }
    }
}
