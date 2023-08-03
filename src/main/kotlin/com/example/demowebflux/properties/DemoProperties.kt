package com.example.demowebflux.properties

import com.example.demowebflux.constants.PREFIX
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.time.DurationMax
import org.hibernate.validator.constraints.time.DurationMin
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@ConfigurationProperties(PREFIX, ignoreUnknownFields = false)
@Validated
data class DemoProperties(
    @field:Min(5)
    val someProp: Int,
    val service: ServiceProperties,
    val client: ClientProperties,
) {
    data class ServiceProperties(
        val cache: Boolean,

        @field:Min(1)
        val multiplier: Int,
    )

    data class ClientProperties(
        @field:NotBlank
        val url: String,

        @field:DurationMin(millis = 100)
        @field:DurationMax(seconds = 60)
        val responseTimeout: Duration,

        @field:DurationMin(millis = 100)
        @field:DurationMax(seconds = 60)
        val connectionTimeout: Duration,

        @field:DurationMin(millis = 100)
        @field:DurationMax(seconds = 60)
        val readTimeout: Duration,

        @field:DurationMin(millis = 100)
        @field:DurationMax(seconds = 60)
        val writeTimeout: Duration,
    )
}
