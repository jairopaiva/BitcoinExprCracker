package com.jairopaiva.exprlab.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NeuralNetworkTest {
    @Test
    fun estimatePrivateKeyClampsToValidRange() {
        assertEquals(1L, TinyMLP.estimatePrivateKey(-1.0, order = 29))
        assertEquals(28L, TinyMLP.estimatePrivateKey(2.0, order = 29))
        assertEquals(12L, TinyMLP.estimatePrivateKey(0.4, order = 29))
    }

    @Test
    fun forwardOutputIsNormalized() {
        val model = TinyMLP(inputSize = 6, hiddenUnits = 8, seed = 1L)
        val output = model.predict(DoubleArray(6) { 0.5 })
        assertTrue(output in 0.0..1.0)
    }
}
