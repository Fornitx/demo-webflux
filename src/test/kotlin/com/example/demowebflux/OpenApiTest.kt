package com.example.demowebflux

import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.TimeUnit

@EnabledOnOs(OS.WINDOWS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class OpenApiTest {
//    @Test
    fun test() {
        TimeUnit.HOURS.sleep(1)
    }
}
