package com.example.demowebflux.data

import com.example.demowebflux.data.validation.NullOrNotEmpty
import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Size

data class DemoRequest(
    @JsonProperty("message")
    @field:Size(min = 3, max = 256)
    val msg: String,

    @field:NullOrNotEmpty
    val tags: Set<String>? = null,

    @get:JsonAnyGetter
    @JsonAnySetter
    val others: Map<String, Any> = mutableMapOf()
)
