package com.example.demowebflux

import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner

abstract class AbstractContextTest : AbstractJUnitTest() {
    protected fun contextRunner(): ReactiveWebApplicationContextRunner {
        return ReactiveWebApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withUserConfiguration(DemoWebfluxApplication::class.java)
    }

    protected fun ReactiveWebApplicationContextRunner.withProfiles(vararg profiles: String): ReactiveWebApplicationContextRunner {
        require(profiles.isNotEmpty())
        return this.withPropertyValues("spring.profiles.active=${profiles.joinToString()}")
    }
}
