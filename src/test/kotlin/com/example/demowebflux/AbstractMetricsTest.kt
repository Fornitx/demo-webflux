package com.example.demowebflux

import com.example.demowebflux.metrics.DemoMetrics
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.fail

abstract class AbstractMetricsTest : AbstractTest() {
    @Autowired
    protected lateinit var meterRegistry: MeterRegistry

    @AfterEach
    fun clearMetrics() {
        meterRegistry.clear()
    }

    protected fun assertNoMeter(name: String) {
        assertNoRawMeter(DemoMetrics.name(name))
    }

    protected fun assertNoRawMeter(name: String) {
        val meters = meterRegistry.find(name).meters()
        assertThat(meters)
            .`as` { "Check meters [${meters.map { it.id }}] is empty for meter '$name'" }
            .isEmpty()
    }

    protected fun assertMeter(name: String, tags: Map<String, String>, count: Int = 1) {
        assertRawMeter(DemoMetrics.name(name), tags, count)
    }

    protected fun assertRawMeter(name: String, tags: Map<String, String>, count: Int = 1) {
        assertRawMeters(name, mapOf(tags to count))
    }

    protected fun assertMeters(name: String, tagsCountMap: Map<Map<String, String>, Int>) {
        assertRawMeters(DemoMetrics.name(name), tagsCountMap)
    }

    protected fun assertRawMeters(name: String, tagsCountMap: Map<Map<String, String>, Int>) {
        val meters = meterRegistry.find(name).meters()
        assertThat(meters)
            .`as` { "Check meters [${meters.map { it.id }}] has size ${tagsCountMap.size} for meter '$name'" }
            .hasSize(tagsCountMap.size)

        for ((tags, count) in tagsCountMap) {
            val matchedMeter = meters.singleOrNull { it.matches(tags) }

            if (matchedMeter == null) {
                fail("Meter $name is not found by tags [$tags]")
            } else if (matchedMeter is Counter) {
                assertEquals(count.toDouble(), matchedMeter.count())
            } else if (matchedMeter is Timer) {
                assertEquals(count.toLong(), matchedMeter.count())
            } else if (matchedMeter is Gauge) {
                assertEquals(count.toDouble(), matchedMeter.value())
            } else {
                fail("Unsupported meter type '${matchedMeter::class}'")
            }
        }
    }

    private fun Meter.matches(tags: Map<String, String>): Boolean {
        return tags.all { tag -> this.hasTag(tag.key, tag.value) }
    }

    private fun Meter.hasTag(key: String, value: String): Boolean {
        return this.id.tags.any { it.matches(key, value) }
    }

    private fun Tag.matches(key: String, value: String): Boolean {
        return this.key == key && this.value == value
    }
}
