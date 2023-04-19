package com.example.demowebflux.properties

import com.example.demowebflux.AbstractContextTest
import com.example.demowebflux.utils.TestProfiles
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException
import org.springframework.boot.context.properties.bind.BindException
import org.springframework.boot.context.properties.bind.validation.BindValidationException

class PropertiesTest : AbstractContextTest() {
    @Test
    fun testAllOk() {
        contextRunner()
            .run { context ->
                assertThat(context.startupFailure).isNull()
            }
    }

    @Test
    fun testPropertyIsInvalid() {
        contextRunner().withProfiles(TestProfiles.SOME_PROP).run { context ->
            assertThat(context.startupFailure)
                .isNotNull()
                .isInstanceOf(ConfigurationPropertiesBindException::class.java)
                .hasMessageContaining("Error creating bean with name 'demo-com.example.demowebflux.properties.DemoProperties': Could not bind properties to 'DemoProperties' : prefix=demo, ignoreInvalidFields=false, ignoreUnknownFields=false")
                .cause()
                .isNotNull()
                .isInstanceOf(BindException::class.java)
                .cause()
                .isNotNull()
                .isInstanceOf(BindValidationException::class.java)
                .hasMessageContaining("Field error in object 'demo' on field 'someProp': rejected value [1]; codes [Min.demo.someProp,Min.someProp,Min.int,Min]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [demo.someProp,someProp]; arguments []; default message [someProp],5]; default message [must be greater than or equal to 5]; origin class path resource [application-someprop.yml]")
        }
    }
}
