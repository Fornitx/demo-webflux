package com.example.demowebflux

import io.micrometer.core.instrument.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractMetricsTest : AbstractJUnitTest() {
    @Autowired
    protected lateinit var meterRegistry: MeterRegistry

    @AfterEach
    fun clearMetrics() {
        meterRegistry.clear()
    }

    protected fun assertNoMeter(name: String) {
        val meters = meterRegistry.find(name).meters()
        assertThat(meters)
            .`as` { "Check meters [${meters.map { it.id }}] is empty" }
            .isEmpty()
    }

    protected fun assertMeter(name: String, tags: Map<String, String>, count: Int = 1) {
        assertMeters(name, mapOf(tags to count))
    }

    protected fun assertMeters(name: String, tagsCountMap: Map<Map<String, String>, Int>) {
        val meters = meterRegistry.find(name).meters()
        assertThat(meters)
            .`as` { "Check meters [${meters.map { it.id }}] has size ${tagsCountMap.size}" }
            .hasSize(tagsCountMap.size)

        for ((tags, count) in tagsCountMap) {
            val matchedMeter = meters.singleOrNull { meter -> meter.matches(tags) }

            if (matchedMeter == null) {
                fail { "Meter $name is not found by tags [$tags]" }
            } else if (matchedMeter is Counter) {
                assertThat(matchedMeter.count()).isEqualTo(count.toDouble())
            } else if (matchedMeter is Timer) {
                assertThat(matchedMeter.count()).isEqualTo(count.toLong())
            } else if (matchedMeter is Gauge) {
                assertThat(matchedMeter.value()).isEqualTo(count.toDouble())
            } else {
                fail { "Unsupported meter type '${matchedMeter::class}'" }
            }
        }
    }

    private fun Meter.matches(tags: Map<String, String>): Boolean {
        return tags.all { tag -> this.hasTag(tag.key, tag.value) }
    }

    private fun Meter.hasTag(key: String, value: String): Boolean {
        return this.id.tags.any { tag -> tag.matches(key, value) }
    }

    private fun Tag.matches(key: String, value: String): Boolean {
        return this.key == key && this.value == value
    }
}
