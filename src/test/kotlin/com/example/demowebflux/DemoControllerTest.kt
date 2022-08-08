package com.example.demowebflux

import com.example.demowebflux.DemoController.Companion.FOO_PATH
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(webEnvironment = RANDOM_PORT)
class DemoControllerTest : BaseDemoTest(FOO_PATH, "123_") {
    override val happyWayMsg = MSG.repeat(3)
}
