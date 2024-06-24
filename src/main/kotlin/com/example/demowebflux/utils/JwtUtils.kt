package com.example.demowebflux.utils

import org.jose4j.jwt.consumer.JwtConsumerBuilder
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

object JwtUtils {
    const val BEARER = "Bearer "

    private val jwtConsumer = JwtConsumerBuilder()
        .setSkipDefaultAudienceValidation()
        .setVerificationKeyResolver { jws, _ ->
            KeyFactory.getInstance(jws.algorithm.keyType)
                .parsePublic("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEpRXnkDDoxpKKh31vqV4lD2cfZ4AZPrjaKe+7tocjRdUvK4PRPjKhW7IRXpBkpsgHieJhyN9n5FTkoIJCtoPSQw==")
        }
        .build()

    fun parseDemoToken(authHeader: String): DemoToken {
        val token = authHeader.removePrefix(BEARER)
        val claims = jwtConsumer.processToClaims(token)
        return DemoToken(
            audiences = claims.audience
        )
    }

    private fun KeyFactory.parsePublic(key: String): PublicKey {
        val keyBytes = Base64.getDecoder().decode(key)
        val keySpec = X509EncodedKeySpec(keyBytes)
        return this.generatePublic(keySpec)
    }
}
