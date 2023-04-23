package com.example.demowebflux.properties

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties("demo", ignoreUnknownFields = false)
@Validated
data class DemoProperties(
    @field:Min(5)
    val someProp: Int,
    val service: ServiceProperties,
) {
    data class ServiceProperties(
        val cache: Boolean,
        @field:Min(1)
        val multiplier: Int,
    )
}
