package com.example.demowebflux.data

import javax.validation.constraints.Size

data class DemoRequest(
    @field:Size(min = 3)
    val msg: String,
)
