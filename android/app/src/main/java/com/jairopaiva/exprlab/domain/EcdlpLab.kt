package com.jairopaiva.exprlab.domain

import java.math.BigInteger
import kotlin.math.ceil
import kotlin.math.sqrt

data class EcdlpResult(
    val method: String,
    val privateKey: Long,
    val steps: Int,
    val elapsedMs: Long
)

data class ComplexityRow(
    val label: String,
    val bits: Int,
    val expectedRhoSteps: String,
    val expectedKangarooSteps: String,
    val feasibleOnDevice: Boolean
)

object EcdlpLab {
    fun pollardRho(curve: ToyCurve, target: ToyPoint, maxIterations: Int = 10_000): EcdlpResult {
        val start = System.nanoTime()
        val order = curve.generatorOrder.toLong()
        val generator = curve.generator

        data class State(val point: ToyPoint?, val a: Long, val b: Long)

        fun normalize(value: Long): Long = Math.floorMod(value, order)
        fun step(state: State): State {
            val slot = Math.floorMod(state.point?.x ?: 0L, 3L).toInt()
            return when (slot) {
                0 -> State(curve.add(state.point, generator), normalize(state.a + 1), state.b)
                1 -> State(curve.add(state.point, state.point), normalize(state.a * 2), normalize(state.b * 2))
                else -> State(curve.add(state.point, target), state.a, normalize(state.b + 1))
            }
        }

        var tortoise = step(State(generator, 1, 0))
        var hare = step(step(State(generator, 1, 0)))
        var steps = 2
        while (tortoise.point != hare.point && steps < maxIterations) {
            tortoise = step(tortoise)
            hare = step(step(hare))
            steps += 2
        }
        require(tortoise.point == hare.point) { "Pollard rho did not cycle within $maxIterations steps" }

        val a = Math.floorMod(tortoise.a - hare.a, order)
        val b = Math.floorMod(tortoise.b - hare.b, order)
        val denominator = modInverse(b, order)
        val key = Math.floorMod(-a * denominator, order)
        require(key in 1L until order) { "Pollard rho failed to recover a valid toy key" }
        return EcdlpResult("Pollard rho", key, steps, elapsedMs(start))
    }

    fun kangaroo(curve: ToyCurve, target: ToyPoint, maxWildSteps: Int = 2_000): EcdlpResult {
        val start = System.nanoTime()
        val order = curve.generatorOrder.toLong()
        val upperBound = order - 1
        val jumpDistances = longArrayOf(1, 1, 2, 2, 3, 3, 4, 5)
        val jumpPoints = jumpDistances.map { curve.multiply(it, curve.generator) }
        val tameSteps = ceil(8.0 * sqrt(upperBound.toDouble())).toInt().coerceAtLeast(16)

        fun jumpIndex(point: ToyPoint?): Int = Math.floorMod((point?.x ?: 0L) + (point?.y ?: 0L), jumpDistances.size.toLong()).toInt()

        var tamePoint = curve.multiply(upperBound, curve.generator)
        var tameDistance = 0L
        val tame = HashMap<ToyPoint?, Long>()
        repeat(tameSteps) {
            val index = jumpIndex(tamePoint)
            tamePoint = curve.add(tamePoint, jumpPoints[index])
            tameDistance += jumpDistances[index]
            tame.putIfAbsent(tamePoint, tameDistance)
        }

        var wildPoint: ToyPoint? = target
        var wildDistance = 0L
        var steps = 0
        var recovered: Long? = null
        while (steps < maxWildSteps && recovered == null) {
            val tameDistanceAtPoint = tame[wildPoint]
            if (tameDistanceAtPoint != null) {
                recovered = Math.floorMod(upperBound + tameDistanceAtPoint - wildDistance, order)
                break
            }
            val index = jumpIndex(wildPoint)
            wildPoint = curve.add(wildPoint, jumpPoints[index])
            wildDistance += jumpDistances[index]
            steps++
        }
        require(recovered != null && recovered in 1L until order) { "Kangaroo did not recover a valid toy key" }
        return EcdlpResult("Kangaroo", recovered, tameSteps + steps, elapsedMs(start))
    }

    fun complexityRows(): List<ComplexityRow> {
        return listOf(8, 16, 32, 64, 128, 256).map { bits ->
            val size = Math.pow(2.0, bits.toDouble())
            val rho = Math.pow(2.0, bits / 2.0)
            ComplexityRow(
                label = if (bits == 256) "secp256k1 (~2^256)" else "toy group ~2^$bits",
                bits = bits,
                expectedRhoSteps = "%.3e".format(rho),
                expectedKangarooSteps = "%.3e".format(rho),
                feasibleOnDevice = bits <= 32
            )
        }
    }

    private fun modInverse(value: Long, modulus: Long): Long {
        val valueBig = BigInteger.valueOf(Math.floorMod(value, modulus))
        val modulusBig = BigInteger.valueOf(modulus)
        return valueBig.modInverse(modulusBig).toString().toLong()
    }

    private fun elapsedMs(start: Long): Long = (System.nanoTime() - start) / 1_000_000
}
