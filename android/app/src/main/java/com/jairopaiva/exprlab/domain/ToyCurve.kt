package com.jairopaiva.exprlab.domain

import java.math.BigInteger
import kotlin.math.sqrt

data class ToyPoint(val x: Long, val y: Long)

data class ToyKeyPair(val privateKey: Long, val publicKey: ToyPoint)

class ToyCurve(
    val p: Long = 23,
    val a: Long = 1,
    val b: Long = 4
) {
    init {
        require(p > 3) { "Toy curve prime must be greater than 3" }
        require((4 * a * a * a + 27 * b * b) % p != 0L) { "Singular curve" }
    }

    private fun mod(value: Long): Long = Math.floorMod(value, p)

    fun isOnCurve(point: ToyPoint): Boolean {
        val rhs = mod(point.x * point.x * point.x + a * point.x + b)
        return mod(point.y * point.y) == rhs
    }

    fun negate(point: ToyPoint?): ToyPoint? = point?.let { ToyPoint(it.x, mod(-it.y)) }

    fun add(left: ToyPoint?, right: ToyPoint?): ToyPoint? {
        if (left == null) return right
        if (right == null) return left
        if (left.x == right.x) {
            val ySum = mod(left.y + right.y)
            if (ySum == 0L) return null
            return double(left)
        }

        val denominator = modInverse(mod(right.x - left.x), p)
        val slope = mod((right.y - left.y) * denominator)
        val x3 = mod(slope * slope - left.x - right.x)
        val y3 = mod(slope * (left.x - x3) - left.y)
        return ToyPoint(x3, y3)
    }

    fun double(point: ToyPoint): ToyPoint? {
        if (point.y == 0L) return null
        val slope = mod((3 * point.x * point.x + a) * modInverse(2 * point.y, p))
        val x3 = mod(slope * slope - 2 * point.x)
        val y3 = mod(slope * (point.x - x3) - point.y)
        return ToyPoint(x3, y3)
    }

    fun multiply(scalar: Long, point: ToyPoint): ToyPoint? {
        if (scalar == 0L) return null
        var result: ToyPoint? = null
        var addend: ToyPoint? = point
        var remaining = scalar
        while (remaining > 0) {
            if ((remaining and 1L) == 1L) result = add(result, addend)
            addend = add(addend, addend)
            remaining = remaining ushr 1
        }
        return result
    }

    fun points(): List<ToyPoint> {
        val out = mutableListOf<ToyPoint>()
        for (x in 0 until p) {
            val rhs = mod(x * x * x + a * x + b)
            for (y in 0 until p) {
                if (mod(y * y) == rhs) out.add(ToyPoint(x, y))
            }
        }
        return out
    }

    fun orderOf(point: ToyPoint): Int {
        var acc: ToyPoint? = null
        var count = 0
        val limit = (p + 2 * sqrt(p.toDouble()) + 10).toInt()
        while (count < limit) {
            acc = add(acc, point)
            count++
            if (acc == null) return count
        }
        return -1
    }

    val generator: ToyPoint by lazy {
        points().maxByOrNull { orderOf(it) } ?: error("Toy curve has no affine points")
    }

    val generatorOrder: Int by lazy { orderOf(generator) }

    fun keyPair(privateKey: Long): ToyKeyPair {
        require(privateKey in 1L until generatorOrder.toLong()) { "Private key must be in 1..${generatorOrder - 1}" }
        val publicKey = multiply(privateKey, generator) ?: error("Invalid public key")
        return ToyKeyPair(privateKey, publicKey)
    }

    fun privateKeyFromPublicKey(publicKey: ToyPoint): Long? {
        for (candidate in 1L until generatorOrder.toLong()) {
            if (multiply(candidate, generator) == publicKey) return candidate
        }
        return null
    }

    fun featuresFor(point: ToyPoint): DoubleArray {
        val rhs = mod(point.x * point.x * point.x + a * point.x + b)
        return doubleArrayOf(
            point.x.toDouble() / p.toDouble(),
            point.y.toDouble() / p.toDouble(),
            mod(point.x * point.x).toDouble() / p.toDouble(),
            mod(point.y * point.y).toDouble() / p.toDouble(),
            mod(point.x * point.y).toDouble() / p.toDouble(),
            rhs.toDouble() / p.toDouble()
        )
    }

    fun allKeyPairs(): List<ToyKeyPair> = (1L until generatorOrder.toLong()).map { keyPair(it) }

    private fun modInverse(value: Long, modulus: Long): Long {
        val valueBig = BigInteger.valueOf(Math.floorMod(value, modulus))
        val modulusBig = BigInteger.valueOf(modulus)
        return valueBig.modInverse(modulusBig).toString().toLong()
    }
}
