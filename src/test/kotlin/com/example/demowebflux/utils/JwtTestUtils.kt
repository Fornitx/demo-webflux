package com.example.demowebflux.utils

import com.example.demowebflux.utils.JwtUtils.BEARER
import org.apache.commons.lang3.RandomStringUtils
import org.jose4j.jws.AlgorithmIdentifiers
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.NumericDate
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.temporal.ChronoUnit.HOURS
import java.util.*

object JwtTestUtils {
    const val PRIVATE_KEY =
        "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDLW+8W4WC+6IxZG4X3D9llqDGr7Ls5n3EXU9TcnLsJuA=="
    const val PUBLIC_KEY =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEpRXnkDDoxpKKh31vqV4lD2cfZ4AZPrjaKe+7tocjRdUvK4PRPjKhW7IRXpBkpsgHieJhyN9n5FTkoIJCtoPSQw=="

    val TOKEN = BEARER + newClaims {
        setAudience("AUD1", "AUD2")
    }

    val TOKEN_NO_AUD = BEARER + newClaims {
        setAudience("AUD3")
    }

    val TOKEN_OVERDUE = BEARER + newOverdueClaims {
        setAudience("AUD1", "AUD2")
    }

    val TOKEN_BAD_SIGN = TOKEN.substring(0, TOKEN.length - 10) + RandomStringUtils.randomAlphanumeric(10)

    fun newClaims(block: JwtClaims.() -> Unit): String {
        val claims = JwtClaims()
        claims.setIssuedAtToNow()
        claims.setExpirationTimeMinutesInTheFuture(60f /* one hour */)

        claims.block()

        return claims.signWith(PRIVATE_KEY)
    }

    fun newOverdueClaims(block: JwtClaims.() -> Unit): String {
        val claims = JwtClaims()
        claims.issuedAt = Instant.now().minus(2, HOURS).toNumericDate()
        claims.expirationTime = Instant.now().minus(1, HOURS).toNumericDate()

        claims.block()

        return claims.signWith(PRIVATE_KEY)
    }

    private fun Instant.toNumericDate(): NumericDate {
        return NumericDate.fromMilliseconds(this.toEpochMilli())
    }

    private fun JwtClaims.signWith(privateKeyBase64: String): String {
        val jws = JsonWebSignature()
        jws.payload = this.toJson()
        jws.key = KeyFactory.getInstance("EC" /* RSA */).parsePrivate(privateKeyBase64)
        jws.algorithmHeaderValue = AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256
//        jws.algorithmHeaderValue = AlgorithmIdentifiers.RSA_USING_SHA256
        return jws.compactSerialization
    }

    private fun KeyFactory.parsePrivate(privateKeyBase64: String): PrivateKey {
        val keyBytes = Base64.getDecoder().decode(privateKeyBase64)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return this.generatePrivate(keySpec)
    }
}
