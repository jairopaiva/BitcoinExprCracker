package com.jairopaiva.exprlab.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GeneticExpressionsTest {
    @Test
    fun geneticSearchReturnsStructuredResult() {
        val privateKey = ByteArray(32).also { it[31] = 1 }
        val publicKey = Secp256k1.compressed(Secp256k1.publicKeyFromPrivateBytes(privateKey))
        val result = GeneticExpressions.run(
            targets = byteArrayOf(0, 0, 0, 1),
            publicKey = publicKey,
            populationSize = 24,
            generations = 4,
            seed = 3L
        )
        assertEquals(4, result.targetCount)
        assertEquals(4, result.bestExpressions.size)
        assertEquals(5, result.generations.size)
    }
}
