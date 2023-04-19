package com.example.demowebflux.metrics

import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.utils.Constants
import io.github.oshai.withLoggingContext
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.util.*

const val METRICS_TAG_PATH = "path"
const val METRICS_TAG_CODE = "code"

@Component
class DemoMetrics(private val meterRegistry: MeterRegistry) {
    fun httpTimings(path: String): Timer {
        return meterRegistry.timer(DemoMetrics::httpTimings.name, METRICS_TAG_PATH, path)
    }

    fun error(demoError: DemoError): Counter {
        return meterRegistry.counter(DemoMetrics::error.name, METRICS_TAG_CODE, demoError.code.toString())
    }

    suspend fun <T> withHttpTimings(
        requestId: UUID,
        userId: String,
        path: String,
        function: suspend () -> T,
    ): T {
        val sample = Timer.start()
        val result = withLoggingContext(
            Constants.LOGSTASH_REQUEST_ID to requestId.toString(),
            Constants.LOGSTASH_USER_ID to userId,
            Constants.LOGSTASH_RELATIVE_PATH to path,
        ) {
            return@withLoggingContext function()
        }
        sample.stop(httpTimings(path))
        return result
    }
}
