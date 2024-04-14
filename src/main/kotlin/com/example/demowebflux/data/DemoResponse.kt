package com.example.demowebflux.data

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Demo Response")
data class DemoResponse(
    @JsonProperty("text")
    val msg: String,

    val instant: Instant = Instant.now(),

    @get:JsonAnyGetter
    @JsonAnySetter
    val others: Map<String, Any> = mutableMapOf()
)
