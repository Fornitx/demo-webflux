package com.example.demowebflux.metrics

import com.example.demowebflux.constants.LOGSTASH_RELATIVE_PATH
import com.example.demowebflux.constants.LOGSTASH_REQUEST_ID
import com.example.demowebflux.constants.LOGSTASH_USER_ID
import com.example.demowebflux.errors.DemoError
import com.github.benmanes.caffeine.cache.Cache
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
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
        return timer("HTTP timings", DemoMetrics::httpTimings.name, METRICS_TAG_PATH, path)
    }

    fun error(demoError: DemoError, status: Int? = null): Counter {
        val finalStatus = status ?: demoError.httpStatus
        return counter(
            "Appliccation errors", DemoMetrics::error.name,
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
        val timer = Timer.start()
        return withLoggingContext(
            LOGSTASH_REQUEST_ID to requestId.toString(),
            LOGSTASH_USER_ID to userId,
            LOGSTASH_RELATIVE_PATH to path,
        ) {
            function()
        }.also {
            timer.stop(httpTimings(path))
        }
    }

    fun cacheHits(cache: Cache<*, *>) {
        gauge("Cache hits", DemoMetrics::cacheHits.name) { cache.stats().hitCount().toDouble() }
    }

    fun cacheMiss(cache: Cache<*, *>) {
        gauge("Cache miss", DemoMetrics::cacheMiss.name) { cache.stats().missCount().toDouble() }
    }

    private final fun counter(description: String, name: String, vararg tags: String): Counter {
        return Counter.builder(name).description(description).tags(*tags).register(meterRegistry)
    }

    private final fun timer(description: String, name: String, vararg tags: String): Timer {
        return Timer.builder(name).description(description).tags(*tags).register(meterRegistry)
    }

    private final fun gauge(
        description: String,
        name: String,
        vararg tags: String,
        numberSupplier: () -> Number
    ): Gauge {
        return Gauge.builder(name, numberSupplier).description(description).tags(*tags).register(meterRegistry)
    }
}
