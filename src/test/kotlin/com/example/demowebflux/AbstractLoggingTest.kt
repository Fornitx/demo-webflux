package com.example.demowebflux

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory

abstract class AbstractLoggingTest : AbstractMetricsTest() {
    private var appender: ListAppender<ILoggingEvent>? = null
    private val logbackRootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger

    @BeforeEach
    fun addAppender() {
        appender = ListAppender()
        appender!!.start()
        logbackRootLogger.addAppender(appender)
    }

    @AfterEach
    fun detachAppender() {
        logbackRootLogger.detachAppender(appender)
    }

    protected fun assertLogger(count: Int) {
        assertThat(appender!!.list).hasSize(count)
    }
}
