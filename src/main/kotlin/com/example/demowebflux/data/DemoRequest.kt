package com.example.demowebflux.data

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.Size

data class DemoRequest(
    @JsonProperty("text")
    @field:Size(min = 3)
    val msg: String,

    @get:JsonAnyGetter
    @JsonAnySetter
    val _anyField: Map<String, Any> = mutableMapOf()
)
