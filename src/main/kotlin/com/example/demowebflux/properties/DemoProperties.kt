package com.example.demowebflux.properties

import com.example.demowebflux.constants.PREFIX_DEMO
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(PREFIX_DEMO, ignoreUnknownFields = false)
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
        val url: String
    )
}
