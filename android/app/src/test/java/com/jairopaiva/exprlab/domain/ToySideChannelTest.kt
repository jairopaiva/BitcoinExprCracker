package com.jairopaiva.exprlab.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToySideChannelTest {
    private val curve = ToyCurve()

    @Test
    fun constantTimeMultiplyMatchesVariableTime() {
        for (key in 1L..10L) {
            val expected = curve.keyPair(key).publicKey
            assertEquals(expected, ToySideChannel.multiplyConstantTime(curve, key, curve.generator))
        }
    }

    @Test
    fun timingReportHasSamplesAndSpreads() {
        val report = ToySideChannel.report(curve, listOf(1L, 7L, 28L), repetitions = 20)
        assertEquals(3, report.samples.size)
        assertTrue(report.averageVariableNs >= 0.0)
        assertTrue(report.averageConstantNs >= 0.0)
        assertTrue(report.variableSpreadNs >= 0)
        assertTrue(report.constantSpreadNs >= 0)
    }
}
