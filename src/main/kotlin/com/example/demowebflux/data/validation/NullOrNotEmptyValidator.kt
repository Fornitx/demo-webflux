package com.example.demowebflux.data.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class NullOrNotEmptyValidator : ConstraintValidator<NullOrNotEmpty?, Any?> {
    override fun isValid(value: Any?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) {
            return true
        }
        if (value is String) {
            return value.isNotEmpty()
        }
        if (value is Collection<*>) {
            return value.isNotEmpty()
        }
        if (value is Map<*, *>) {
            return value.isNotEmpty()
        }
        return false
    }
}
