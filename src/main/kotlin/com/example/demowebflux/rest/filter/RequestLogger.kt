package com.example.demowebflux.rest.filter

import com.example.demowebflux.constants.LOGSTASH_RELATIVE_PATH
import com.example.demowebflux.constants.LOGSTASH_REQUEST_ID
import io.github.oshai.kotlinlogging.KLogging
import io.github.oshai.kotlinlogging.withLoggingContext

object RequestLogger : KLogging() {
    fun logRequest(requestId: String?, relativePath: String?, body: String? = null) {
        withLoggingContext(
            LOGSTASH_REQUEST_ID to requestId,
            LOGSTASH_RELATIVE_PATH to relativePath,
        ) {
            if (logger.isDebugEnabled) {
                logger.debug("Request [body={}]", body)
            } else {
                logger.info("Request")
            }
        }
    }

    fun logResponse(requestId: String?, relativePath: String?, httpStatus: Int?, body: String? = null) {
        withLoggingContext(
            LOGSTASH_REQUEST_ID to requestId,
            LOGSTASH_RELATIVE_PATH to relativePath,
        ) {
            if (logger.isDebugEnabled) {
                logger.debug("Response [httpStatus={}, body={}]", httpStatus, body)
            } else {
                logger.info("Response [httpStatus={}]", httpStatus)
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
            if (logger.isDebugEnabled) {
                logger.error(
                    "Response [httpStatus={}, errorCode={}, body={}]",
                    httpStatus,
                    errorCode,
                    body,
                    error
                )
            } else {
                logger.error("Response [httpStatus={}, errorCode={}]", httpStatus, errorCode, error)
            }
        }
    }
}
