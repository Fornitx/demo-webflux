package com.example.demowebflux.data

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class DemoResponse(
    @JsonProperty("text")
    val msg: String,

    val instant: Instant = Instant.now(),

    @get:JsonAnyGetter
    @JsonAnySetter
    val others: Map<String, Any> = mutableMapOf()
)
