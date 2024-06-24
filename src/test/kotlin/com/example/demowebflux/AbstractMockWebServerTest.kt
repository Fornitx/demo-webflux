package com.example.demowebflux

import com.example.demowebflux.constants.PREFIX
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockWebServer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.util.TestSocketUtils

abstract class AbstractMockWebServerTest : AbstractWebTestClientTest() {
    companion object {
        @JvmStatic
        protected val SERVER_PORT = TestSocketUtils.findAvailableTcpPort()

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("$PREFIX.client.url") { "http://localhost:$SERVER_PORT" }
        }

        @JvmStatic
        protected fun mockWebServer(port: Int, dispatcher: Dispatcher, block: suspend () -> Unit) =
            MockWebServer().use { server ->
                server.dispatcher = dispatcher
                server.start(port)
                runTest {
                    block()
                }
            }
    }
}
