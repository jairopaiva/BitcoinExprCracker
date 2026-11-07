package com.jairopaiva.exprlab.domain

import kotlin.random.Random

sealed interface ExprNode {
    fun eval(x: ByteArray, y: ByteArray): Int
}

data class VarX(val index: Int) : ExprNode {
    override fun eval(x: ByteArray, y: ByteArray): Int = x[index].toInt() and 0xff
}

data class VarY(val index: Int) : ExprNode {
    override fun eval(x: ByteArray, y: ByteArray): Int = y[index].toInt() and 0xff
}

data class Const(val value: Int) : ExprNode {
    override fun eval(x: ByteArray, y: ByteArray): Int = value and 0xff
}

data class UnaryExpr(val op: Char, val child: ExprNode) : ExprNode {
    override fun eval(x: ByteArray, y: ByteArray): Int = when (op) {
        '~' -> child.eval(x, y).inv() and 0xff
        else -> throw IllegalArgumentException("Unsupported unary operator: $op")
    }
}

data class BinaryExpr(val op: Char, val left: ExprNode, val right: ExprNode) : ExprNode {
    override fun eval(x: ByteArray, y: ByteArray): Int {
        val l = left.eval(x, y)
        val r = right.eval(x, y)
        val value = when (op) {
            '+' -> l + r
            '-' -> l - r
            '*' -> l * r
            '%' -> if (r == 0) 0 else l % r
            '&' -> l and r
            '|' -> l or r
            '^' -> l xor r
            else -> throw IllegalArgumentException("Unsupported binary operator: $op")
        }
        return value and 0xff
    }
}

data class GeneticResult(
    val bestExpressions: List<String>,
    val bestFitness: Int,
    val targetCount: Int,
    val generations: List<Int>
)

object GeneticExpressions {
    private val binaryOps = charArrayOf('+', '-', '*', '%', '&', '|', '^')

    fun randomNode(random: Random, depth: Int): ExprNode {
        if (depth <= 0 || random.nextDouble() < 0.28) {
            return if (random.nextBoolean()) {
                if (random.nextBoolean()) VarX(random.nextInt(32)) else VarY(random.nextInt(32))
            } else Const(random.nextInt(256))
        }
        return if (random.nextDouble() < 0.22) {
            UnaryExpr('~', randomNode(random, depth - 1))
        } else {
            BinaryExpr(binaryOps[random.nextInt(binaryOps.size)], randomNode(random, depth - 1), randomNode(random, depth - 1))
        }
    }

    fun fold(node: ExprNode): ExprNode = when (node) {
        is Const, is VarX, is VarY -> node
        is UnaryExpr -> {
            val child = fold(node.child)
            if (child is Const) Const(UnaryExpr(node.op, child).eval(ByteArray(0), ByteArray(0))) else UnaryExpr(node.op, child)
        }
        is BinaryExpr -> {
            val left = fold(node.left)
            val right = fold(node.right)
            if (left is Const && right is Const) Const(BinaryExpr(node.op, left, right).eval(ByteArray(0), ByteArray(0))) else BinaryExpr(node.op, left, right)
        }
    }

    fun mutate(node: ExprNode, random: Random): ExprNode {
        if (random.nextDouble() < 0.32) return fold(randomNode(random, 2))
        return when (node) {
            is UnaryExpr -> UnaryExpr(node.op, mutate(node.child, random))
            is BinaryExpr -> {
                val op = if (random.nextDouble() < 0.12) binaryOps[random.nextInt(binaryOps.size)] else node.op
                BinaryExpr(op, mutate(node.left, random), mutate(node.right, random))
            }
            else -> if (random.nextDouble() < 0.2) randomNode(random, 1) else node
        }.let(::fold)
    }

    fun crossover(left: ExprNode, right: ExprNode, random: Random): ExprNode {
        if (left is BinaryExpr && right is BinaryExpr && random.nextDouble() < 0.7) {
            return BinaryExpr(
                left.op,
                crossover(left.left, right.left, random),
                crossover(left.right, right.right, random)
            ).let(::fold)
        }
        return mutate(if (random.nextBoolean()) left else right, random)
    }

    fun render(node: ExprNode): String = when (node) {
        is VarX -> "X[${node.index}]"
        is VarY -> "Y[${node.index}]"
        is Const -> "0x${node.value.toString(16)}"
        is UnaryExpr -> "(~${render(node.child)})"
        is BinaryExpr -> "(${render(node.left)} ${node.op} ${render(node.right)})"
    }

    private fun fitness(individual: List<ExprNode>, targets: ByteArray, x: ByteArray, y: ByteArray): Int {
        var exact = 0
        var absolute = 0
        for (index in targets.indices) {
            val result = individual[index].eval(x, y)
            val target = targets[index].toInt() and 0xff
            if (result == target) exact++
            absolute += kotlin.math.abs(result - target)
        }
        return exact * 1_000 - absolute
    }

    private fun coordinates(publicKey: ByteArray): Pair<ByteArray, ByteArray> {
        val point = Secp256k1.decompress(publicKey) ?: error("Invalid public key")
        return with(Secp256k1) {
            point.x.toFixedSizeByteArray(32) to point.y.toFixedSizeByteArray(32)
        }
    }

    fun run(
        targets: ByteArray,
        publicKey: ByteArray,
        populationSize: Int = 48,
        generations: Int = 12,
        seed: Long = 1L
    ): GeneticResult {
        require(targets.isNotEmpty() && targets.size <= 8) { "Use 1..8 target bytes" }
        val random = Random(seed)
        val (x, y) = coordinates(publicKey)
        var population = List(populationSize) { List(targets.size) { fold(randomNode(random, 3)) } }
        val history = mutableListOf<Int>()
        var best: List<ExprNode>? = null
        var bestFitness = Int.MIN_VALUE

        for (generation in 0..generations) {
            val scored = population.map { individual -> individual to fitness(individual, targets, x, y) }
            scored.maxByOrNull { it.second }?.let { (individual, score) ->
                if (score > bestFitness) {
                    bestFitness = score
                    best = individual
                }
            }
            history.add(scored.maxOf { it.second })
            if (generation == generations) break

            val sorted = scored.sortedByDescending { it.second }
            val elite = sorted.take(4).map { it.first }
            val next = elite.toMutableList()
            while (next.size < populationSize) {
                fun pick(): List<ExprNode> {
                    val candidates = List(3) { sorted[random.nextInt(sorted.size.coerceAtLeast(1))].first }
                    return candidates.maxByOrNull { fitness(it, targets, x, y) } ?: candidates.first()
                }
                val child = List(targets.size) { index ->
                    val gene = crossover(pick()[index], pick()[index], random)
                    if (random.nextDouble() < 0.35) mutate(gene, random) else gene
                }
                next.add(child)
            }
            population = next
        }

        val winner = best ?: error("Genetic search produced no individual")
        return GeneticResult(winner.map(::render), bestFitness, targets.size, history)
    }
}
