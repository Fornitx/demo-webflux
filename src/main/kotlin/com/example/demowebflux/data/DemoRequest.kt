package com.example.demowebflux.data

import com.example.demowebflux.data.validation.NullOrNotBlank
import com.example.demowebflux.data.validation.NullOrNotEmpty
import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Demo Request")
data class DemoRequest(
    @Schema(description = "Field must be null or not blank")
    @JsonProperty("nullOrNotBlankStrRenamed")
    @field:Size(min = 3)
    @field:NullOrNotBlank
    val nullOrNotBlankStr: String? = null,

    @Schema(description = "Set must be null or not empty")
    @field:NullOrNotEmpty
    val nullOrNotEmptySet: Set<String>? = null,

    @get:JsonAnyGetter
    @JsonAnySetter
    val others: Map<String, Any> = mutableMapOf()
)
