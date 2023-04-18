package com.example.demowebflux

import io.github.oshai.KotlinLogging
import kotlin.reflect.jvm.jvmName

abstract class AbstractJUnitTest {
    protected val log = KotlinLogging.logger(this::class.jvmName)
}
