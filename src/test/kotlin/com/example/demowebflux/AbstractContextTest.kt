package com.example.demowebflux

import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner

abstract class AbstractContextTest : AbstractJUnitTest() {
    protected fun contextRunner(): ApplicationContextRunner {
        return ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withUserConfiguration(DemoWebfluxApplication::class.java)
    }

    protected fun ApplicationContextRunner.withProfiles(vararg profiles: String): ApplicationContextRunner {
        require(profiles.isNotEmpty())
        return this.withPropertyValues("spring.profiles.active=${profiles.joinToString()}")
    }
}
