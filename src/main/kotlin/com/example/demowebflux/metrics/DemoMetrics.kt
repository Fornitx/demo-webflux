package com.example.demowebflux.metrics

import com.example.demowebflux.constants.LOGSTASH_RELATIVE_PATH
import com.example.demowebflux.constants.LOGSTASH_REQUEST_ID
import com.example.demowebflux.constants.LOGSTASH_USER_ID
import com.example.demowebflux.errors.DemoError
import com.github.benmanes.caffeine.cache.Cache
import io.github.oshai.withLoggingContext
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.util.*

const val METRICS_TAG_PATH = "path"
const val METRICS_TAG_CODE = "code"
const val METRICS_TAG_STATUS = "status"

@Component
class DemoMetrics(private val meterRegistry: MeterRegistry) {
    fun httpTimings(path: String): Timer {
        return meterRegistry.timer(DemoMetrics::httpTimings.name, METRICS_TAG_PATH, path)
    }

    fun error(demoError: DemoError, status: Int? = null): Counter {
        val finalStatus = status ?: demoError.httpStatus
        return meterRegistry.counter(
            DemoMetrics::error.name,
            METRICS_TAG_CODE, demoError.code.toString(),
            METRICS_TAG_STATUS, finalStatus.toString(),
        )
    }

    final inline fun <T> withHttpTimings(
        requestId: UUID,
        userId: String,
        path: String,
        function: () -> T,
    ): T {
        val sample = Timer.start()
        return withLoggingContext(
            LOGSTASH_REQUEST_ID to requestId.toString(),
            LOGSTASH_USER_ID to userId,
            LOGSTASH_RELATIVE_PATH to path,
        ) {
            function()
        }.also {
            sample.stop(httpTimings(path))
        }
    }

    fun cacheHits(cache: Cache<*, *>) {
        meterRegistry.gauge(DemoMetrics::cacheHits.name, cache) { it.stats().hitCount().toDouble() }
    }

    fun cacheMiss(cache: Cache<*, *>) {
        meterRegistry.gauge(DemoMetrics::cacheMiss.name, cache) { it.stats().missCount().toDouble() }
    }
}
