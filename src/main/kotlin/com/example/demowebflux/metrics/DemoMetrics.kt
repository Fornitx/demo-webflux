package com.example.demowebflux.metrics

import com.example.demowebflux.constants.LOGSTASH_RELATIVE_PATH
import com.example.demowebflux.constants.LOGSTASH_REQUEST_ID
import com.example.demowebflux.constants.LOGSTASH_USER_ID
import com.example.demowebflux.errors.DemoError
import com.google.common.base.CaseFormat.LOWER_CAMEL
import com.google.common.base.CaseFormat.LOWER_UNDERSCORE
import io.github.oshai.kotlinlogging.coroutines.withLoggingContextAsync
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component

const val METRICS_TAG_PATH = "path"
const val METRICS_TAG_CODE = "code"
const val METRICS_TAG_STATUS = "status"

@Component
class DemoMetrics(private val registry: MeterRegistry) {
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

    final suspend inline fun <T> withHttpTimings(
        requestId: String,
        userId: String,
        path: String,
        crossinline block: suspend () -> T,
    ): T {
        val timer = Timer.start()
        return withLoggingContextAsync(
            LOGSTASH_REQUEST_ID to requestId,
            LOGSTASH_USER_ID to userId,
            LOGSTASH_RELATIVE_PATH to path,
        ) {
            block()
        }.also {
            timer.stop(httpTimings(path))
        }
    }

    private fun counter(description: String, name: String, vararg tags: String): Counter {
        return Counter.builder(name(name)).description(description).tags(*tags).register(registry)
    }

    private fun timer(description: String, name: String, vararg tags: String): Timer {
        return Timer.builder(name(name)).description(description).tags(*tags).register(registry)
    }

    private fun gauge(
        description: String,
        name: String,
        vararg tags: String,
        numberSupplier: () -> Number
    ): Gauge {
        return Gauge.builder(name(name), numberSupplier).description(description).tags(*tags).register(registry)
    }

    companion object {
        fun name(name: String): String {
            return "demo_" + LOWER_CAMEL.to(LOWER_UNDERSCORE, name)
        }
    }
}
