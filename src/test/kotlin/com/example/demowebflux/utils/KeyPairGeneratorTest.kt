package com.example.demowebflux.utils

import org.jose4j.jwk.EcJwkGenerator
import org.jose4j.jwk.PublicJsonWebKey
import org.jose4j.jwk.RsaJwkGenerator
import org.jose4j.keys.EllipticCurves
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.util.*

class KeyPairGeneratorTest {
    fun generateKeyPair(): List<Arguments> {
        return listOf(
            arguments(
                named("RSA", KeyPairGenerator.getInstance("RSA")
                    .apply { initialize(2048) }
                    .generateKeyPair()
                )
            ),
            arguments(
                named("EC", KeyPairGenerator.getInstance("EC")
                    .apply { initialize(ECGenParameterSpec("secp256r1")) }
                    .generateKeyPair()
                )
            ),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun generateKeyPair(keyPair: KeyPair) {
        printKey(keyPair.private)
        printKey(keyPair.public)
    }

    fun generateKeyPairByJose4j(): List<Arguments> {
        return listOf(
            arguments(named("RSA", RsaJwkGenerator.generateJwk(2048))),
            arguments(named("EC", EcJwkGenerator.generateJwk(EllipticCurves.P256))),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun generateKeyPairByJose4j(keyPair: PublicJsonWebKey) {
        printKey(keyPair.privateKey)
        printKey(keyPair.publicKey)
    }

    private fun printKey(key: Key) {
        if (key is PrivateKey) {
            println("Private key:")
            println(key.toBase64())
        } else if (key is PublicKey) {
            println("Public key:")
            println(key.toBase64())
        }
    }

    private fun Key.toBase64(): String {
        return Base64.getEncoder().encodeToString(this.encoded)
    }
}
