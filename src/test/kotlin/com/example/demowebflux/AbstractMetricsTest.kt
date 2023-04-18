package com.example.demowebflux

import io.micrometer.core.instrument.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import kotlin.collections.Map.Entry

abstract class AbstractMetricsTest {
    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @AfterEach
    fun clearMetrics() {
        meterRegistry.clear()
    }

    protected fun assertNoMeter(name: String) {
        val meters = meterRegistry.find(name).meters()
        assertThat(meters)
            .`as`("Check meters [%s] is empty", meters.map { it.id })
            .isEmpty()
    }

    protected fun assertMeter(name: String, tags: Map<String, String>, count: Int = 1) {
        assertMeters(name, mapOf(tags to count))
    }

    protected fun assertMeters(name: String, tagsCountMap: Map<Map<String, String>, Int>) {
        val meters = meterRegistry.find(name).meters()
        assertThat(meters)
            .`as`("Check meters [%s] has size %d", meters.map { it.id }, tagsCountMap.size)
            .hasSize(tagsCountMap.size)

        for ((tags, count) in tagsCountMap) {
            val matchedMeter = meters.singleOrNull { meter -> meter.matches(tags) }

            if (matchedMeter == null) {
                fail("Meter %s is not found by tags [%s]", name, tags)
            } else if (matchedMeter is Counter) {
                assertThat(matchedMeter.count()).isEqualTo(count.toDouble())
            } else if (matchedMeter is Timer) {
                assertThat(matchedMeter.count()).isEqualTo(count.toLong())
            } else {
                fail("Unsupported meter type '${matchedMeter::class}'")
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
