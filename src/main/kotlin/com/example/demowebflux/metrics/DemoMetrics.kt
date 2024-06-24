package com.example.demowebflux.metrics

import com.example.demowebflux.constants.PREFIX
import com.example.demowebflux.errors.DemoError
import com.example.demowebflux.rest.exceptions.WebClientConnectException
import com.example.demowebflux.rest.exceptions.WebClientTimeoutException
import com.google.common.base.CaseFormat.LOWER_CAMEL
import com.google.common.base.CaseFormat.LOWER_UNDERSCORE
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.netty.handler.timeout.TimeoutException
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponseException
import org.springframework.web.reactive.function.client.WebClientRequestException
import reactor.netty.http.client.PrematureCloseException
import java.net.ConnectException
import java.util.concurrent.ConcurrentHashMap

const val METRICS_TAG_HTTP_METHOD = "status"
const val METRICS_TAG_URI = "uri"
const val METRICS_TAG_HTTP_STATUS = "status"
const val METRICS_TAG_CODE = "code"

@Component
class DemoMetrics(private val registry: MeterRegistry) {
    fun httpServerRequests(httpMethod: HttpMethod, uri: String): Counter = counter(
        "HTTP server requests",
        DemoMetrics::httpServerRequests.name,
        METRICS_TAG_HTTP_METHOD, httpMethod.name(),
        METRICS_TAG_URI, uri,
    )

    fun httpServerResponses(httpMethod: HttpMethod, uri: String, httpStatus: HttpStatusCode?): Timer = timer(
        "HTTP server responses",
        DemoMetrics::httpServerResponses.name,
        METRICS_TAG_HTTP_METHOD, httpMethod.name(),
        METRICS_TAG_URI, uri,
        METRICS_TAG_HTTP_STATUS, httpStatus?.value().toString(),
    )

    fun httpClientRequests(httpMethod: HttpMethod, uri: String): Counter = counter(
        "HTTP client requests",
        DemoMetrics::httpClientRequests.name,
        METRICS_TAG_HTTP_METHOD, httpMethod.name(),
        METRICS_TAG_URI, uri,
    )

    fun httpClientResponses(httpMethod: HttpMethod, uri: String, httpStatus: HttpStatusCode?): Timer = timer(
        "HTTP client responses",
        DemoMetrics::httpClientResponses.name,
        METRICS_TAG_HTTP_METHOD, httpMethod.name(),
        METRICS_TAG_URI, uri,
        METRICS_TAG_HTTP_STATUS, httpStatus?.value().toString(),
    )

    fun error(demoError: DemoError, status: Int? = null): Counter = counter(
        "Application errors",
        DemoMetrics::error.name,
        METRICS_TAG_CODE, demoError.code.toString(),
        METRICS_TAG_HTTP_STATUS, (status ?: demoError.httpStatus).toString(),
    )

    suspend fun <T> httpClientCallWithMetrics(
        method: HttpMethod,
        uri: String,
        block: suspend () -> ResponseEntity<T>,
    ): ResponseEntity<T> {
        httpClientRequests(method, uri).increment()

        val timer = Timer.start()
        return try {
            block().also {
                timer.stop(httpClientResponses(method, uri, it.statusCode))
            }
        } catch (ex: Exception) {
            val httpStatus = if (ex is ErrorResponseException) ex.statusCode else null
            timer.stop(httpClientResponses(method, uri, httpStatus))

            if (ex is WebClientRequestException) {
                if (ex.cause is ConnectException || ex.cause is PrematureCloseException) {
                    throw WebClientConnectException(ex)
                }
                if (ex.cause is TimeoutException) {
                    throw WebClientTimeoutException(ex)
                }
            }
            throw ex
        }
    }

    private fun counter(description: String, name: String, vararg tags: String): Counter =
        Counter.builder(name(name)).description(description).tags(*tags).register(registry)

    private fun timer(description: String, name: String, vararg tags: String): Timer =
        Timer.builder(name(name)).description(description).tags(*tags).register(registry)

    private fun gauge(
        description: String,
        name: String,
        vararg tags: String,
        numberSupplier: () -> Number
    ): Gauge = Gauge.builder(name(name), numberSupplier).description(description).tags(*tags).register(registry)

    companion object {
        private val nameCache = ConcurrentHashMap<String, String>()

        fun name(name: String): String = nameCache.computeIfAbsent(name) {
            PREFIX + "_" + LOWER_CAMEL.to(LOWER_UNDERSCORE, name)
        }
    }
}
