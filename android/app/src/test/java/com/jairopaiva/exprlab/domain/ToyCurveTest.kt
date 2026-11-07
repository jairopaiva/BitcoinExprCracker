package com.jairopaiva.exprlab.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToyCurveTest {
    private val curve = ToyCurve()

    @Test
    fun toyCurveHasPrimeOrder29() {
        assertEquals(29, curve.generatorOrder)
    }

    @Test
    fun toyKeyPairsAreOnCurve() {
        val pair = curve.keyPair(7L)
        assertTrue(curve.isOnCurve(pair.publicKey))
    }

    @Test
    fun bruteForceRecoversEveryToyPrivateKey() {
        for (privateKey in 1L until curve.generatorOrder.toLong()) {
            val pair = curve.keyPair(privateKey)
            assertEquals(privateKey.toLong(), curve.privateKeyFromPublicKey(pair.publicKey)!!.toLong())
        }
    }
}
