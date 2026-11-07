package com.jairopaiva.exprlab.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Secp256k1Test {
    private val privateKeyOne = ByteArray(32).also { it[31] = 1 }

    private fun smallPrivateKey(value: Int): ByteArray = ByteArray(32).also { it[31] = value.toByte() }

    @Test
    fun privateKeyOneHasKnownCompressedPublicKey() {
        val point = Secp256k1.publicKeyFromPrivateBytes(privateKeyOne)
        assertEquals(
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798",
            Hex.encode(Secp256k1.compressed(point))
        )
    }

    @Test
    fun compressedPublicKeyRoundTrips() {
        val point = Secp256k1.publicKeyFromPrivateBytes(privateKeyOne)
        val compressed = Secp256k1.compressed(point)
        val decoded = Secp256k1.decompress(compressed)
        assertTrue(decoded != null)
        assertEquals(point.x, decoded!!.x)
        assertEquals(point.y, decoded.y)
    }

    @Test
    fun privateKeyOneHasKnownWif() {
        assertEquals(
            "KwDiBf89QgGbjEhKnhXJuH7LrciVrZi3qYjgd9M7rFU73sVHnoWn",
            Secp256k1.privateKeyToWif(privateKeyOne)
        )
    }

    @Test
    fun randomPrivateKeysProduceOnCurvePublicKeys() {
        repeat(3) {
            val privateKey = Secp256k1.randomPrivateKey()
            val point = Secp256k1.publicKeyFromPrivateBytes(privateKey)
            assertTrue(Secp256k1.isOnCurve(point))
            assertEquals(point, Secp256k1.decompress(Secp256k1.compressed(point)))
        }
    }

    @Test
    fun smallScalarsProduceOnCurvePublicKeys() {
        for (value in 1..20) {
            val point = Secp256k1.publicKeyFromPrivateBytes(smallPrivateKey(value))
            assertTrue(Secp256k1.isOnCurve(point))
            assertEquals(point, Secp256k1.decompress(Secp256k1.compressed(point)))
        }
    }
}
