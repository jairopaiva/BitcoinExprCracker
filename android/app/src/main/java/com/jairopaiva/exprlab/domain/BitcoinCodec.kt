package com.jairopaiva.exprlab.domain

import java.math.BigInteger
import java.security.MessageDigest
import org.bouncycastle.crypto.digests.RIPEMD160Digest

object BitcoinCodec {
    private val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray()
    private val BECH32_CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l".toCharArray()
    private val BECH32_GENERATOR = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)

    fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    fun hash160(data: ByteArray): ByteArray {
        val sha = sha256(data)
        val digest = RIPEMD160Digest()
        digest.update(sha, 0, sha.size)
        val out = ByteArray(20)
        digest.doFinal(out, 0)
        return out
    }

    fun base58Encode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        var zeros = 0
        while (zeros < input.size && input[zeros] == 0.toByte()) zeros++

        var number = BigInteger(1, input)
        if (number.signum() == 0) return "1".repeat(zeros)

        val digits = StringBuilder()
        val fiftyEight = BigInteger.valueOf(58)
        while (number.signum() > 0) {
            val remainder = number.remainder(fiftyEight).toString().toInt()
            digits.append(BASE58_ALPHABET[remainder])
            number = number.divide(fiftyEight)
        }

        return "1".repeat(zeros) + digits.reverse().toString()
    }

    fun base58CheckEncode(payload: ByteArray): String {
        val checksum = sha256(sha256(payload)).copyOfRange(0, 4)
        return base58Encode(payload + checksum)
    }

    fun p2pkhAddress(compressedPublicKey: ByteArray, testnet: Boolean = false): String {
        val version = if (testnet) 0x6f.toByte() else 0x00.toByte()
        return base58CheckEncode(byteArrayOf(version) + hash160(compressedPublicKey))
    }

    fun p2wpkhAddress(compressedPublicKey: ByteArray, testnet: Boolean = false): String {
        val hrp = if (testnet) "tb" else "bc"
        val program = hash160(compressedPublicKey)
        return bech32Encode(hrp, 0, program)
    }

    fun bech32Encode(hrp: String, version: Int, program: ByteArray): String {
        require(version in 0..16) { "Invalid witness version" }
        val converted = convertBits(program, 8, 5, true) ?: error("Invalid program")
        val data = IntArray(converted.size + 1)
        data[0] = version
        for (i in converted.indices) data[i + 1] = converted[i]
        val checksum = bech32Checksum(hrp, data)
        val combined = data + checksum
        return buildString {
            append(hrp)
            append('1')
            for (value in combined) append(BECH32_CHARSET[value])
        }
    }

    private fun bech32Polymod(values: IntArray): Int {
        var chk = 1L
        for (value in values) {
            val top = chk ushr 25
            chk = (chk shl 5) xor value.toLong()
            for (i in 0 until 5) {
                if (((top ushr i) and 1L) == 1L) chk = chk xor BECH32_GENERATOR[i].toLong()
            }
        }
        return chk.toInt()
    }

    private fun bech32Checksum(hrp: String, data: IntArray): IntArray {
        val values = hrpExpand(hrp) + data + IntArray(6)
        val polymod = bech32Polymod(values) xor 1
        return IntArray(6) { i -> (polymod ushr (5 * (5 - i))) and 31 }
    }

    private fun hrpExpand(hrp: String): IntArray {
        val out = IntArray(hrp.length * 2 + 1)
        for (i in hrp.indices) out[i] = hrp[i].code - 33
        for (i in hrp.indices) out[hrp.length + 1 + i] = hrp[i].code and 31
        out[hrp.length] = 0
        return out
    }

    private fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): IntArray? {
        var acc = 0
        var bits = 0
        val result = mutableListOf<Int>()
        val maxv = (1 shl toBits) - 1
        val maxAcc = (1 shl (fromBits + toBits - 1)) - 1
        for (byte in data) {
            val value = byte.toInt() and 0xff
            if (value < 0 || value ushr fromBits != 0) return null
            acc = ((acc shl fromBits) or value) and maxAcc
            bits += fromBits
            while (bits >= toBits) {
                bits -= toBits
                result.add((acc ushr bits) and maxv)
            }
        }
        if (pad && bits > 0) result.add((acc shl (toBits - bits)) and maxv)
        return if (bits >= fromBits) null else result.toIntArray()
    }
}
