package com.jairopaiva.exprlab.domain

import kotlin.math.exp
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

data class NeuralConfig(
    val hiddenUnits: Int = 16,
    val epochs: Int = 600,
    val learningRate: Double = 0.08,
    val seed: Long = 42L,
    val validationEvery: Int = 5
)

data class NeuralSample(val features: DoubleArray, val target: Double) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NeuralSample) return false
        return features.contentEquals(other.features) && target == other.target
    }

    override fun hashCode(): Int {
        var result = features.contentHashCode()
        result = 31 * result + target.hashCode()
        return result
    }
}

data class LossPoint(val epoch: Int, val trainLoss: Double, val validationLoss: Double)

data class TrainingResult(
    val history: List<LossPoint>,
    val finalTrainLoss: Double,
    val finalValidationLoss: Double,
    val validationExactRate: Double,
    val validationCount: Int
)

class TinyMLP(inputSize: Int, hiddenUnits: Int, seed: Long) {
    private val random = Random(seed)
    private val hiddenWeights = Array(hiddenUnits) { DoubleArray(inputSize) }
    private val hiddenBias = DoubleArray(hiddenUnits)
    private val outputWeights = DoubleArray(hiddenUnits)
    private val outputBias = DoubleArray(1)

    init {
        val hiddenScale = 1.0 / sqrt(inputSize.toDouble())
        val outputScale = 1.0 / sqrt(hiddenUnits.toDouble())
        for (h in 0 until hiddenUnits) {
            for (i in 0 until inputSize) hiddenWeights[h][i] = random.nextDouble(-hiddenScale, hiddenScale)
            hiddenBias[h] = random.nextDouble(-hiddenScale, hiddenScale)
            outputWeights[h] = random.nextDouble(-outputScale, outputScale)
        }
        outputBias[0] = 0.0
    }

    private data class ForwardCache(
        val input: DoubleArray,
        val hidden: DoubleArray,
        val output: Double
    )

    private fun forward(input: DoubleArray): ForwardCache {
        require(input.size == hiddenWeights[0].size) { "Unexpected input size" }
        val hidden = DoubleArray(hiddenWeights.size)
        for (h in hidden.indices) {
            var sum = hiddenBias[h]
            val weights = hiddenWeights[h]
            for (i in input.indices) sum += weights[i] * input[i]
            hidden[h] = tanh(sum)
        }
        var outputSum = outputBias[0]
        for (h in hidden.indices) outputSum += outputWeights[h] * hidden[h]
        return ForwardCache(input.copyOf(), hidden, sigmoid(outputSum))
    }

    fun predict(input: DoubleArray): Double = forward(input).output

    private fun tanh(value: Double): Double = kotlin.math.tanh(value)

    private fun sigmoid(value: Double): Double = 1.0 / (1.0 + exp(-value))

    fun train(
        samples: List<NeuralSample>,
        config: NeuralConfig,
        curveOrder: Int,
        onEpoch: ((LossPoint) -> Unit)? = null
    ): TrainingResult {
        require(samples.isNotEmpty()) { "No training samples" }
        val shuffled = samples.shuffled(Random(config.seed))
        val validation = shuffled.filterIndexed { index, _ -> index % config.validationEvery == 0 }
        val training = shuffled.filterIndexed { index, _ -> index % config.validationEvery != 0 }
        require(training.isNotEmpty() && validation.isNotEmpty()) { "Need both training and validation samples" }

        val gradHiddenWeights = Array(hiddenWeights.size) { DoubleArray(hiddenWeights[it].size) }
        val gradHiddenBias = DoubleArray(hiddenBias.size)
        val gradOutputWeights = DoubleArray(outputWeights.size)
        val gradOutputBias = DoubleArray(1)
        val history = mutableListOf<LossPoint>()
        var finalTrainLoss = 0.0
        var finalValidationLoss = 0.0

        for (epoch in 1..config.epochs) {
            for (h in gradHiddenWeights.indices) gradHiddenWeights[h].fill(0.0)
            gradHiddenBias.fill(0.0)
            gradOutputWeights.fill(0.0)
            gradOutputBias.fill(0.0)

            for (sample in training) {
                val cache = forward(sample.features)
                val error = cache.output - sample.target
                val deltaOutput = error * cache.output * (1.0 - cache.output)
                gradOutputBias[0] += deltaOutput
                for (h in outputWeights.indices) {
                    gradOutputWeights[h] += deltaOutput * cache.hidden[h]
                    val deltaHidden = deltaOutput * outputWeights[h] * (1.0 - cache.hidden[h] * cache.hidden[h])
                    gradHiddenBias[h] += deltaHidden
                    for (i in sample.features.indices) gradHiddenWeights[h][i] += deltaHidden * sample.features[i]
                }
            }

            val scale = config.learningRate / training.size
            for (h in outputWeights.indices) {
                outputWeights[h] -= scale * gradOutputWeights[h]
                hiddenBias[h] -= scale * gradHiddenBias[h]
                for (i in hiddenWeights[h].indices) hiddenWeights[h][i] -= scale * gradHiddenWeights[h][i]
            }
            outputBias[0] -= scale * gradOutputBias[0]

            finalTrainLoss = training.meanSquaredError()
            finalValidationLoss = validation.meanSquaredError()
            val point = LossPoint(epoch, finalTrainLoss, finalValidationLoss)
            history.add(point)
            onEpoch?.invoke(point)
        }

        val exact = validation.count { sample ->
            val estimate = estimatePrivateKey(predict(sample.features), curveOrder)
            estimate == (sample.target * curveOrder).roundToLong()
        }
        return TrainingResult(
            history = history,
            finalTrainLoss = finalTrainLoss,
            finalValidationLoss = finalValidationLoss,
            validationExactRate = if (validation.isEmpty()) 0.0 else exact.toDouble() / validation.size,
            validationCount = validation.size
        )
    }

    private fun List<NeuralSample>.meanSquaredError(): Double {
        var sum = 0.0
        for (sample in this) {
            val prediction = predict(sample.features)
            val error = prediction - sample.target
            sum += error * error
        }
        return sum / size
    }

    companion object {
        fun estimatePrivateKey(normalizedPrediction: Double, order: Int): Long {
            val clamped = normalizedPrediction.coerceIn(0.0, 1.0)
            return ((clamped * order.toDouble()).roundToLong()).coerceIn(1L, (order - 1).toLong())
        }
    }
}
