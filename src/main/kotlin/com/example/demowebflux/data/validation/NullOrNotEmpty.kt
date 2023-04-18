package com.example.demowebflux.data.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@MustBeDocumented
@Constraint(validatedBy = [NullOrNotEmptyValidator::class])
annotation class NullOrNotEmpty(
    val message: String = "{javax.validation.constraints.NullOrNotEmpty.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
