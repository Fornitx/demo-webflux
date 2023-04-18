package com.example.demowebflux.properties

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties("demo", ignoreUnknownFields = false)
@Validated
data class DemoProperties(
    @field:Min(5)
    val someProp: Int,
)
