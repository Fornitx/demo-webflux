package com.example.demowebflux.properties

import com.example.demowebflux.AbstractContextTest
import com.example.demowebflux.utils.TestProfiles
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PropertiesTest : AbstractContextTest() {
    @Test
    fun testAllOk() {
        contextRunner().run { context ->
            assertNull(context.startupFailure)
        }
    }

    @Test
    fun testPropertyIsInvalid() {
        contextRunner().withProfiles(TestProfiles.SOME_PROP).run { context ->
            // TODO check error
            assertNotNull(context.startupFailure)
        }
    }
}
