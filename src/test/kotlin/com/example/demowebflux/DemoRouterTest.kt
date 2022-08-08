package com.example.demowebflux

import com.example.demowebflux.DemoRouter.Companion.BAR_PATH
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(webEnvironment = RANDOM_PORT)
class DemoRouterTest : BaseDemoTest(BAR_PATH, "abc") {
    override val happyWayMsg = "ABC~ABC~ABC"
}
