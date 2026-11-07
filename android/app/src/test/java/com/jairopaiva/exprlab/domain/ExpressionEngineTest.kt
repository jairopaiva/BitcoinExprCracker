package com.jairopaiva.exprlab.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionEngineTest {
    private val privateKeyOne = ByteArray(32).also { it[31] = 1 }

    @Test
    fun generatedExpressionsAreDeterministic() {
        val publicKey = Secp256k1.compressed(Secp256k1.publicKeyFromPrivateBytes(privateKeyOne))
        val first = ExpressionEngine.generateSamples(publicKey, seed = 1337, count = 4, operations = 4)
        val second = ExpressionEngine.generateSamples(publicKey, seed = 1337, count = 4, operations = 4)
        assertEquals(first.map { it.result }, second.map { it.result })
        assertEquals(first.map { it.text }, second.map { it.text })
    }

    @Test
    fun selfTestReconstructsSuppliedPrivateKey() {
        val publicKey = Secp256k1.compressed(Secp256k1.publicKeyFromPrivateBytes(privateKeyOne))
        val result = ExpressionEngine.findTryes(privateKeyOne, publicKey, maxTries = 20_000)
        assertTrue("Self-test should complete", result.completed)
        val (candidate, _) = ExpressionEngine.reconstructPrivateKey(publicKey, result.tryes)
        assertArrayEquals(privateKeyOne, candidate)
    }
}
