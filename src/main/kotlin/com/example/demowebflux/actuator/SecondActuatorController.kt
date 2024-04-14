package com.example.demowebflux.actuator

import io.swagger.v3.oas.annotations.Operation
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.actuate.health.HttpCodeStatusMapper
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SecondActuatorController(
    private val healthEndpoint: HealthEndpoint,
    private val httpCodeStatusMapper: HttpCodeStatusMapper,
) {
    @Operation(deprecated = true)
    @GetMapping(
        "/actuator/health/liveness",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun liveness(): ResponseEntity<*> {
        val health = healthEndpoint.healthForPath("liveness")
        val statusCode = httpCodeStatusMapper.getStatusCode(health.status)
        return ResponseEntity.status(statusCode).body(health)
    }

    @Operation(deprecated = true)
    @GetMapping(
        "/actuator/health/readiness",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun readiness(): ResponseEntity<*> {
        val health = healthEndpoint.healthForPath("readiness")
        val statusCode = httpCodeStatusMapper.getStatusCode(health.status)
        return ResponseEntity.status(statusCode).body(health)
    }
}
