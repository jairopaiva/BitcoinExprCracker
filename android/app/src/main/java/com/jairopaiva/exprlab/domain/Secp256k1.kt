package com.jairopaiva.exprlab.domain

import java.math.BigInteger
import java.security.SecureRandom

data class Secp256k1Point(val x: BigInteger, val y: BigInteger)

object Secp256k1 {
    private val P = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16)
    val N: BigInteger = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16)
    private val GX = BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16)
    private val GY = BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16)
    private val SEVEN = BigInteger.valueOf(7)
    private val ONE = BigInteger.ONE
    private val ZERO = BigInteger.ZERO
    private val TWO = BigInteger.valueOf(2)
    private val THREE = BigInteger.valueOf(3)
    private val FOUR = BigInteger.valueOf(4)

    private data class JacobianPoint(val x: BigInteger, val y: BigInteger, val z: BigInteger) {
        fun isInfinity(): Boolean = z.signum() == 0
    }

    private val generatorJacobian = JacobianPoint(GX, GY, ONE)
    private val infinity = JacobianPoint(ZERO, ONE, ZERO)

    private fun mod(value: BigInteger): BigInteger = value.mod(P)

    private fun jacobianDouble(point: JacobianPoint): JacobianPoint {
        if (point.isInfinity() || point.y.signum() == 0) return infinity

        val y2 = mod(point.y.multiply(point.y))
        val c = mod(y2.multiply(y2))
        val x2 = mod(point.x.multiply(point.x))
        val d = mod(TWO.multiply(mod((point.x.add(y2)).multiply(point.x.add(y2)).subtract(x2).subtract(c))))
        val e = mod(THREE.multiply(x2))
        val f = mod(e.multiply(e).subtract(TWO.multiply(d)))
        val x3 = f
        val y3 = mod(e.multiply(d.subtract(f)).subtract(BigInteger.valueOf(8).multiply(c)))
        val z3 = mod(TWO.multiply(point.y).multiply(point.z))
        return JacobianPoint(x3, y3, z3)
    }

    private fun jacobianAdd(left: JacobianPoint, right: JacobianPoint): JacobianPoint {
        if (left.isInfinity()) return right
        if (right.isInfinity()) return left

        val z1z1 = mod(left.z.multiply(left.z))
        val z2z2 = mod(right.z.multiply(right.z))
        val u1 = mod(left.x.multiply(z2z2))
        val u2 = mod(right.x.multiply(z1z1))
        val s1 = mod(left.y.multiply(right.z).multiply(z2z2))
        val s2 = mod(right.y.multiply(left.z).multiply(z1z1))

        if (u1 == u2) {
            return if (s1 == s2) jacobianDouble(left) else infinity
        }

        val h = mod(u2.subtract(u1))
        val h2 = mod(h.multiply(h))
        val i = mod(FOUR.multiply(h2))
        val j = mod(h.multiply(i))
        val r = mod(TWO.multiply(s2.subtract(s1)))
        val v = mod(u1.multiply(i))
        val x3 = mod(r.multiply(r).subtract(j).subtract(TWO.multiply(v)))
        val y3 = mod(r.multiply(v.subtract(x3)).subtract(TWO.multiply(s1).multiply(j)))
        val z3 = mod(TWO.multiply(left.z).multiply(right.z).multiply(h))
        return JacobianPoint(x3, y3, z3)
    }

    private fun jacobianToAffine(point: JacobianPoint): Secp256k1Point? {
        if (point.isInfinity()) return null
        val zInv = point.z.modInverse(P)
        val zInv2 = mod(zInv.multiply(zInv))
        val zInv3 = mod(zInv2.multiply(zInv))
        return Secp256k1Point(mod(point.x.multiply(zInv2)), mod(point.y.multiply(zInv3)))
    }

    private fun scalarMultiplyJacobian(scalar: BigInteger): JacobianPoint {
        require(scalar.signum() > 0) { "Scalar must be positive" }
        var result = infinity
        var addend = generatorJacobian
        for (bit in scalar.bitLength() - 1 downTo 0) {
            result = jacobianDouble(result)
            if (scalar.testBit(bit)) result = jacobianAdd(result, addend)
        }
        return result
    }

    fun scalarMultiply(scalar: BigInteger): Secp256k1Point? = jacobianToAffine(scalarMultiplyJacobian(scalar))

    fun isValidPrivateKey(bytes: ByteArray): Boolean {
        if (bytes.size != 32) return false
        val value = bigIntegerFromBytes(bytes)
        return value.signum() > 0 && value < N
    }

    fun publicKeyFromPrivateBytes(privateKey: ByteArray): Secp256k1Point {
        require(isValidPrivateKey(privateKey)) { "Invalid secp256k1 private key" }
        return scalarMultiply(bigIntegerFromBytes(privateKey)) ?: error("Invalid public key")
    }

    fun isOnCurve(point: Secp256k1Point): Boolean {
        val lhs = mod(point.y.multiply(point.y))
        val rhs = mod(point.x.multiply(point.x).multiply(point.x).add(SEVEN))
        return lhs == rhs
    }

    fun compressed(point: Secp256k1Point): ByteArray {
        require(isOnCurve(point)) { "Point is not on secp256k1" }
        val out = ByteArray(33)
        out[0] = if (point.y.testBit(0)) 0x03 else 0x02
        val xBytes = point.x.toFixedSizeByteArray(32)
        System.arraycopy(xBytes, 0, out, 1, 32)
        return out
    }

    fun uncompressed(point: Secp256k1Point): ByteArray {
        require(isOnCurve(point)) { "Point is not on secp256k1" }
        val out = ByteArray(65)
        out[0] = 0x04
        val xBytes = point.x.toFixedSizeByteArray(32)
        val yBytes = point.y.toFixedSizeByteArray(32)
        System.arraycopy(xBytes, 0, out, 1, 32)
        System.arraycopy(yBytes, 0, out, 33, 32)
        return out
    }

    fun decompress(bytes: ByteArray): Secp256k1Point? {
        return when {
            bytes.size == 33 && (bytes[0] == 0x02.toByte() || bytes[0] == 0x03.toByte()) -> {
                val x = bigIntegerFromBytes(bytes.copyOfRange(1, 33))
                if (x >= P) return null
                val rhs = mod(x.multiply(x).multiply(x).add(SEVEN))
                var y = rhs.modPow((P.add(ONE)).divide(FOUR), P)
                val odd = bytes[0] == 0x03.toByte()
                if (y.testBit(0) != odd) y = P.subtract(y)
                Secp256k1Point(x, y).takeIf(::isOnCurve)
            }
            bytes.size == 65 && bytes[0] == 0x04.toByte() -> {
                val x = bigIntegerFromBytes(bytes.copyOfRange(1, 33))
                val y = bigIntegerFromBytes(bytes.copyOfRange(33, 65))
                if (x >= P || y >= P) return null
                Secp256k1Point(x, y).takeIf(::isOnCurve)
            }
            else -> null
        }
    }

    fun randomPrivateKey(random: SecureRandom = SecureRandom()): ByteArray {
        while (true) {
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            if (isValidPrivateKey(bytes)) return bytes
        }
    }

    fun bigIntegerFromBytes(bytes: ByteArray): BigInteger {
        return BigInteger(1, bytes)
    }

    fun BigInteger.toFixedSizeByteArray(size: Int): ByteArray {
        val raw = toByteArray()
        if (raw.size == size) return raw
        val out = ByteArray(size)
        if (raw.size > size) {
            System.arraycopy(raw, raw.size - size, out, 0, size)
        } else {
            System.arraycopy(raw, 0, out, size - raw.size, raw.size)
        }
        return out
    }

    fun privateKeyToWif(privateKey: ByteArray, compressed: Boolean = true, testnet: Boolean = false): String {
        val version = if (testnet) 0xef.toByte() else 0x80.toByte()
        val payload = ByteArray(if (compressed) 34 else 33)
        payload[0] = version
        System.arraycopy(privateKey, 0, payload, 1, 32)
        if (compressed) payload[33] = 0x01
        return BitcoinCodec.base58CheckEncode(payload)
    }

    fun publicKeyToP2pkhAddress(publicKey: ByteArray, testnet: Boolean = false): String {
        val point = decompress(publicKey) ?: error("Invalid public key")
        val compressedBytes = compressed(point)
        return BitcoinCodec.p2pkhAddress(compressedBytes, testnet)
    }

    fun publicKeyToP2wpkhAddress(publicKey: ByteArray, testnet: Boolean = false): String {
        val point = decompress(publicKey) ?: error("Invalid public key")
        val compressedBytes = compressed(point)
        return BitcoinCodec.p2wpkhAddress(compressedBytes, testnet)
    }
}
