package com.jairopaiva.exprlab.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EcdlpLabTest {
    private val curve = ToyCurve()

    @Test
    fun pollardRhoRecoversToyPrivateKeys() {
        for (key in listOf(1L, 2L, 7L, 13L, 28L)) {
            val pair = curve.keyPair(key)
            val result = EcdlpLab.pollardRho(curve, pair.publicKey)
            assertEquals(key, result.privateKey)
            assertTrue(result.steps > 0)
        }
    }

    @Test
    fun kangarooRecoversToyPrivateKeys() {
        for (key in listOf(1L, 2L, 7L, 13L, 28L)) {
            val pair = curve.keyPair(key)
            val result = EcdlpLab.kangaroo(curve, pair.publicKey)
            assertEquals(key, result.privateKey)
            assertTrue(result.steps > 0)
        }
    }

    @Test
    fun complexityRowsIncludeSecp256k1() {
        val rows = EcdlpLab.complexityRows()
        assertEquals(6, rows.size)
        assertTrue(rows.last().label.contains("secp256k1"))
        assertTrue(rows.last().expectedRhoSteps.contains("e"))
    }
}
