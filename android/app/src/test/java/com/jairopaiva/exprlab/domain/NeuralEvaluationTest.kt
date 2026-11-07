package com.jairopaiva.exprlab.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NeuralEvaluationTest {
    private val curve = ToyCurve()

    @Test
    fun evaluationReportCoversKFoldAblationsAndConfusion() {
        val report = NeuralEvaluation.evaluate(curve, hiddenUnits = 8, epochs = 20, folds = 2, seed = 7L)
        assertEquals(2, report.kFoldExactRates.size)
        assertTrue(report.kFoldExactRates.all { it in 0.0..1.0 })
        assertEquals(4, report.ablations.size)
        assertEquals(28, report.confusion.total)
        assertTrue(report.confusion.exactRate in 0.0..1.0)
        assertTrue(report.confusion.topConfusions.size <= 5)
    }
}
