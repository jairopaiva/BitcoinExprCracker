package com.jairopaiva.exprlab.domain

import java.math.BigInteger

data class GeneratedExpression(val result: Int, val text: String, val operations: Int)

data class ExpressionParameters(
    val index: Int,
    val multiplier: Long,
    val seedSum: Long
)

data class SelfTestResult(
    val tryes: LongArray,
    val expressions: List<String>,
    val completed: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SelfTestResult) return false
        return tryes.contentEquals(other.tryes) && expressions == other.expressions && completed == other.completed
    }

    override fun hashCode(): Int {
        var result = tryes.contentHashCode()
        result = 31 * result + expressions.hashCode()
        result = 31 * result + completed.hashCode()
        return result
    }
}

object ExpressionEngine {
    private const val UINT_MASK = 0xFFFFFFFFL
    private val UINT_SPACE = BigInteger.valueOf(1L shl 32)
    private val OPERATORS_ALL = charArrayOf('+', '-', '*', '%', '&', '|', '~', '^')
    private val OPERATORS_BINARY = charArrayOf('+', '-', '*', '%', '&', '|', '^')

    private data class Operand(val text: String, val value: Int)

    private fun modU32(value: Long): Long = value and UINT_MASK

    private fun addU32(vararg values: Long): Long {
        var acc = 0L
        for (value in values) acc = modU32(acc + value)
        return acc
    }

    private fun mulU32(left: Long, right: Long): Long = modU32(left * right)

    private fun mix64(value: Long): Long {
        var z = value
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }

    private fun nextRandom(seed: Long, insideTry: Long, elementsAdded: Long): Long {
        return mix64(seed + insideTry * -7046029254386353131L + elementsAdded)
    }

    fun extractXY(publicKeyBytes: ByteArray): Pair<ByteArray, ByteArray> {
        return when {
            publicKeyBytes.size == 64 -> {
                publicKeyBytes.copyOfRange(0, 32) to publicKeyBytes.copyOfRange(32, 64)
            }
            publicKeyBytes.size == 65 && publicKeyBytes[0] == 0x04.toByte() -> {
                publicKeyBytes.copyOfRange(1, 33) to publicKeyBytes.copyOfRange(33, 65)
            }
            else -> {
                val point = Secp256k1.decompress(publicKeyBytes)
                    ?: throw IllegalArgumentException("Expected compressed/uncompressed secp256k1 public key or raw X||Y")
                val uncompressed = Secp256k1.uncompressed(point)
                uncompressed.copyOfRange(1, 33) to uncompressed.copyOfRange(33, 65)
            }
        }
    }

    fun parametersFor(publicKeyBytes: ByteArray): Array<ExpressionParameters> {
        val (x, y) = extractXY(publicKeyBytes)
        require(x.size == 32 && y.size == 32) { "Public key coordinates must be 32 bytes" }
        return Array(32) { index ->
            val chunk = index / 4
            val targetX = uint32FromBytes(x, chunk)
            val targetY = uint32FromBytes(y, chunk)
            val product = BigInteger.valueOf(targetX)
                .multiply(BigInteger.valueOf(targetY))
                .mod(UINT_SPACE)
                .toString()
                .toLong()
            val seedX = uint32FromBytes(x, index % 8)
            val seedY = uint32FromBytes(y, index % 8)
            ExpressionParameters(
                index = index,
                multiplier = product,
                seedSum = modU32(seedX + seedY)
            )
        }
    }

    fun generateSamples(publicKeyBytes: ByteArray, seed: Long, count: Int, operations: Int): List<GeneratedExpression> {
        require(count in 1..256) { "count must be in 1..256" }
        require(operations in 1..8) { "operations must be in 1..8" }
        val (x, y) = extractXY(publicKeyBytes)
        return List(count) { offset -> generate(seed + offset, operations, x, y) }
    }

    fun generate(seed: Long, operations: Int, x: ByteArray, y: ByteArray): GeneratedExpression {
        require(operations in 1..8) { "operations must be in 1..8" }
        require(x.size == 32 && y.size == 32) { "X/Y coordinates must be 32 bytes" }

        var insideTry = 0L
        var elementsAdded = 0L
        val results = IntArray(operations)
        val joins = CharArray(maxOf(0, operations - 1))
        val text = StringBuilder()

        for (operationIndex in 0 until operations) {
            var operator = nextOperator(podeNot = true, seed, insideTry, elementsAdded)
            elementsAdded++
            var left = nextVariable(seed, insideTry, elementsAdded, x, y)
            elementsAdded++
            var leftText = left.text
            var leftValue = left.value

            if (operator == '~') {
                leftValue = leftValue xor 0xff
                leftText = "~$leftText"
                operator = nextOperator(podeNot = false, seed, insideTry, elementsAdded)
                elementsAdded++
            }

            val right = nextVariable(seed, insideTry, elementsAdded, x, y)
            elementsAdded++
            val result = applyOperator(operator, leftValue, right.value)
            results[operationIndex] = result

            text.append('(').append(leftText).append(' ').append(operator).append(' ').append(right.text).append(')')
            if (operationIndex < operations - 1) {
                val join = nextOperator(podeNot = false, seed, insideTry, elementsAdded)
                elementsAdded++
                joins[operationIndex] = join
                text.append(' ').append(join).append(' ')
            }
        }

        var acc = results[0]
        for (index in 0 until operations - 1) {
            acc = applyOperator(joins[index], acc, results[index + 1])
        }
        return GeneratedExpression(acc, text.toString(), operations)
    }

    fun findTryes(
        privateKey: ByteArray,
        publicKey: ByteArray,
        maxTries: Long = 200_000,
        onProgress: ((index: Int, tries: Long) -> Unit)? = null
    ): SelfTestResult {
        require(privateKey.size == 32) { "Private key must be 32 bytes" }
        val (x, y) = extractXY(publicKey)
        val parameters = parametersFor(publicKey)
        val tryes = LongArray(32) { -1L }
        val expressions = MutableList<String?>(32) { null }

        for (index in 0 until 32) {
            val target = privateKey[index].toInt() and 0xff
            var found = false
            var tries = 0L
            while (tries < maxTries) {
                val candidate = generateCandidate(parameters[index], tries, x, y)
                if (candidate.result == target) {
                    tryes[index] = tries
                    expressions[index] = candidate.text
                    found = true
                    break
                }
                tries++
                if (tries % 20_000 == 0L) onProgress?.invoke(index, tries)
            }
            onProgress?.invoke(index, tries)
            if (!found) {
                return SelfTestResult(tryes, expressions.map { it.orEmpty() }, completed = false)
            }
        }
        return SelfTestResult(tryes, expressions.map { it.orEmpty() }, completed = true)
    }

    fun reconstructPrivateKey(publicKey: ByteArray, tryes: LongArray): Pair<ByteArray, List<String>> {
        require(tryes.size == 32) { "tryes must contain 32 values" }
        val (x, y) = extractXY(publicKey)
        val parameters = parametersFor(publicKey)
        val out = ByteArray(32)
        val expressions = mutableListOf<String>()
        for (index in 0 until 32) {
            val candidate = generateCandidate(parameters[index], tryes[index], x, y)
            out[index] = candidate.result.toByte()
            expressions.add(candidate.text)
        }
        return out to expressions
    }

    private fun generateCandidate(parameters: ExpressionParameters, tryValue: Long, x: ByteArray, y: ByteArray): GeneratedExpression {
        val tryMod = modU32(tryValue)
        val indexed = modU32(parameters.index.toLong() + tryMod)
        val hash = modU32(tryMod + mulU32(parameters.multiplier, indexed))
        val operations = if ((hash and 1L) == 0L) 2 else 4
        val seedC = modU32(tryMod + parameters.multiplier)
        val seed = addU32(hash, parameters.seedSum, seedC, tryMod)
        return generate(seed, operations, x, y)
    }

    private fun nextOperator(podeNot: Boolean, seed: Long, insideTry: Long, elementsAdded: Long): Char {
        var skip = 0L
        while (true) {
            val hash = nextRandom(seed, insideTry + skip, elementsAdded)
            val index = Math.floorMod(hash, OPERATORS_ALL.size.toLong()).toInt()
            val operator = OPERATORS_ALL[index]
            if (podeNot || operator != '~') return operator
            skip++
        }
    }

    private fun nextVariable(seed: Long, insideTry: Long, elementsAdded: Long, x: ByteArray, y: ByteArray): Operand {
        val hash = nextRandom(seed + 0x632BE59BD9B4E019L, insideTry, elementsAdded)
        val index = Math.floorMod(hash, 32L).toInt()
        val useX = Math.floorMod(index.toLong() + insideTry, 2L) == 0L
        return if (useX) {
            Operand("X[$index]", x[index].toInt() and 0xff)
        } else {
            Operand("Y[$index]", y[index].toInt() and 0xff)
        }
    }

    private fun applyOperator(operator: Char, left: Int, right: Int): Int {
        val value = when (operator) {
            '+' -> left + right
            '-' -> left - right
            '*' -> left * right
            '%' -> if (right == 0) 0 else left % right
            '&' -> left and right
            '|' -> left or right
            '^' -> left xor right
            '~' -> left.inv()
            else -> throw IllegalArgumentException("Unsupported operator: $operator")
        }
        return value and 0xff
    }

    private fun uint32FromBytes(bytes: ByteArray, chunkIndex: Int): Long {
        val offset = chunkIndex * 4
        return ((bytes[offset].toInt() and 0xff).toLong() shl 24) or
            ((bytes[offset + 1].toInt() and 0xff).toLong() shl 16) or
            ((bytes[offset + 2].toInt() and 0xff).toLong() shl 8) or
            (bytes[offset + 3].toInt() and 0xff).toLong()
    }
}
