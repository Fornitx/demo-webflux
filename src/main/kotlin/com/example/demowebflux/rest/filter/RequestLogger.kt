package com.example.demowebflux.rest.filter

import com.example.demowebflux.constants.LOGSTASH_RELATIVE_PATH
import com.example.demowebflux.constants.LOGSTASH_REQUEST_ID
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext

object RequestLogger {
    val log = KotlinLogging.logger { }

    fun logRequest(requestId: String?, relativePath: String?, body: String? = null) {
        withLoggingContext(
            LOGSTASH_REQUEST_ID to requestId,
            LOGSTASH_RELATIVE_PATH to relativePath,
        ) {
            if (log.isDebugEnabled()) {
                log.debug { "Request [body=$body]" }
            } else {
                log.info { "Request" }
            }
        }
    }

    fun logResponse(requestId: String?, relativePath: String?, httpStatus: Int?, body: String? = null) {
        withLoggingContext(
            LOGSTASH_REQUEST_ID to requestId,
            LOGSTASH_RELATIVE_PATH to relativePath,
        ) {
            if (log.isDebugEnabled()) {
                log.debug { "Response [httpStatus=$httpStatus, body=$body]" }
            } else {
                log.info { "Response [httpStatus=$httpStatus]" }
            }
        }
    }

    fun logErrorResponse(
        requestId: String?,
        relativePath: String?,
        httpStatus: Int,
        errorCode: Int,
        error: Throwable,
        body: String? = null
    ) {
        withLoggingContext(
            LOGSTASH_REQUEST_ID to requestId,
            LOGSTASH_RELATIVE_PATH to relativePath,
        ) {
            if (log.isDebugEnabled()) {
                log.error(error) { "Response [httpStatus=$httpStatus, errorCode=$errorCode, body=$body]" }
            } else {
                log.error(error) { "Response [httpStatus=$httpStatus, errorCode=$errorCode]" }
            }
        }
    }
}
