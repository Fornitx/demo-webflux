package com.example.demowebflux.utils

import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.consumer.InvalidJwtException
import org.jose4j.jwt.consumer.InvalidJwtSignatureException
import org.jose4j.jwt.consumer.JwtConsumerBuilder
import org.jose4j.jwx.JsonWebStructure
import org.jose4j.keys.resolvers.VerificationKeyResolver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.security.Key
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertTrue

class VerifySignatureTest {
    @Test
    fun test() {
        val consumer = JwtConsumerBuilder()
            .setVerificationKeyResolver(VerificationKeyResolverImpl(JwtTestUtils.PUBLIC_KEY))
            .setSkipDefaultAudienceValidation()
            .build()

        assertDoesNotThrow { consumer.processToClaims(unbearer(JwtTestUtils.TOKEN)) }
        assertDoesNotThrow { consumer.processToClaims(unbearer(JwtTestUtils.TOKEN_NO_AUD)) }

        val exception = assertThrows<InvalidJwtException> {
            consumer.processToClaims(unbearer(JwtTestUtils.TOKEN_OVERDUE))
        }
        assertTrue(exception.hasExpired())

        assertThrows<InvalidJwtSignatureException> { consumer.processToClaims(unbearer(JwtTestUtils.TOKEN_BAD_SIGN)) }
    }

    private fun unbearer(token: String): String {
        return token.removePrefix(JwtUtils.BEARER)
    }

    class VerificationKeyResolverImpl(publicKeyBase64: String) : VerificationKeyResolver {
        private val keySpec: X509EncodedKeySpec
        private val keyMap: MutableMap<String, PublicKey> = ConcurrentHashMap()

        init {
            val keyBytes = Base64.getDecoder().decode(publicKeyBase64)
            keySpec = X509EncodedKeySpec(keyBytes)
        }

        override fun resolveKey(jws: JsonWebSignature, context: MutableList<JsonWebStructure>): Key {
            val keyType = jws.keyType
            return keyMap.computeIfAbsent(keyType) { key -> KeyFactory.getInstance(key).generatePublic(keySpec) }
        }
    }
}
