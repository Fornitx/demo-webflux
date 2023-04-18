package com.example.demowebflux.properties

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("demo")
data class DemoProperties(
    @field:Min(1)
    val someProp: Int,
)
