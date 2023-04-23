package com.example.demowebflux.properties

import com.example.demowebflux.AbstractContextTest
import com.example.demowebflux.utils.TestProfiles
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.validation.BindValidationException

class PropertiesTest : AbstractContextTest() {
    @Test
    fun allOk() {
        contextRunner()
            .run { context ->
                assertThat(context.startupFailure).isNull()
            }
    }

    @Test
    fun somePropIsInvalid() {
        contextRunner().withProfiles(TestProfiles.SOME_PROP).run { context ->
            assertThat(context.startupFailure)
                .isNotNull()
                .rootCause()
                .isNotNull()
                .isInstanceOf(BindValidationException::class.java)
                .hasMessageContaining("Field error in object 'demo' on field 'someProp': rejected value [1]; codes [Min.demo.someProp,Min.someProp,Min.int,Min]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [demo.someProp,someProp]; arguments []; default message [someProp],5]; default message [must be greater than or equal to 5]")
        }
    }

    @Test
    fun multiplierIsInvalid() {
        contextRunner().withPropertyValues("demo.service.multiplier=-1").run { context ->
            assertThat(context.startupFailure)
                .isNotNull()
                .rootCause()
                .isNotNull()
                .isInstanceOf(BindValidationException::class.java)
                .hasMessageContaining("Field error in object 'demo.service' on field 'multiplier': rejected value [-1]; codes [Min.demo.service.multiplier,Min.multiplier,Min.int,Min]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [demo.service.multiplier,multiplier]; arguments []; default message [multiplier],1]; default message [must be greater than or equal to 1]")
        }
    }
}
