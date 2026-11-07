package com.jairopaiva.exprlab.domain

data class TimingSample(val key: Long, val variableNs: Long, val constantNs: Long)

data class SideChannelReport(
    val samples: List<TimingSample>,
    val averageVariableNs: Double,
    val averageConstantNs: Double,
    val variableSpreadNs: Long,
    val constantSpreadNs: Long
)

object ToySideChannel {
    fun multiplyVariableTime(curve: ToyCurve, scalar: Long, point: ToyPoint): ToyPoint? = curve.multiply(scalar, point)

    fun multiplyConstantTime(curve: ToyCurve, scalar: Long, point: ToyPoint): ToyPoint? {
        val bits = 8
        var result: ToyPoint? = null
        var addend: ToyPoint? = point
        repeat(bits) { bit ->
            val sum = curve.add(result, addend)
            result = if (((scalar shr bit) and 1L) == 1L) sum else result
            addend = curve.add(addend, addend)
        }
        return result
    }

    fun measure(curve: ToyCurve, key: Long, repetitions: Int = 200): TimingSample {
        val pair = curve.keyPair(key)
        var sink = 0L
        repeat(20) {
            sink += multiplyVariableTime(curve, key, pair.publicKey)?.x ?: 0L
            sink += multiplyConstantTime(curve, key, pair.publicKey)?.x ?: 0L
        }
        val variableStart = System.nanoTime()
        repeat(repetitions) { sink += multiplyVariableTime(curve, key, pair.publicKey)?.x ?: 0L }
        val variableNs = (System.nanoTime() - variableStart) / repetitions
        val constantStart = System.nanoTime()
        repeat(repetitions) { sink += multiplyConstantTime(curve, key, pair.publicKey)?.x ?: 0L }
        val constantNs = (System.nanoTime() - constantStart) / repetitions
        return TimingSample(key, variableNs, constantNs).also { sink.hashCode() }
    }

    fun report(curve: ToyCurve, keys: List<Long>, repetitions: Int = 200): SideChannelReport {
        val samples = keys.map { measure(curve, it, repetitions) }
        val variable = samples.map { it.variableNs }
        val constant = samples.map { it.constantNs }
        return SideChannelReport(
            samples = samples,
            averageVariableNs = variable.average(),
            averageConstantNs = constant.average(),
            variableSpreadNs = variable.max() - variable.min(),
            constantSpreadNs = constant.max() - constant.min()
        )
    }

    fun defaultKeys(curve: ToyCurve): List<Long> = listOf(1L, 2L, 3L, 5L, 8L, 13L, 21L, 28L).filter { it < curve.generatorOrder }
}
