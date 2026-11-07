package com.jairopaiva.exprlab.domain

enum class FeatureMode(val label: String) {
    XY("x+y features"),
    X_ONLY("x only"),
    Y_ONLY("y only"),
    SUM("x+y mixed")
}

data class EvalSample(val key: Long, val features: DoubleArray, val target: Double) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EvalSample) return false
        return key == other.key && features.contentEquals(other.features) && target == other.target
    }
    override fun hashCode(): Int = 31 * key.hashCode() + features.contentHashCode()
}

data class ConfusionPair(val trueKey: Long, val estimatedKey: Long, val count: Int)

data class ConfusionReport(val exactRate: Double, val total: Int, val topConfusions: List<ConfusionPair>)

data class NeuralEvalReport(
    val kFoldExactRates: List<Double>,
    val ablations: Map<FeatureMode, Double>,
    val confusion: ConfusionReport
)

object NeuralEvaluation {
    fun featuresFor(curve: ToyCurve, point: ToyPoint, mode: FeatureMode): DoubleArray {
        val base = curve.featuresFor(point)
        return when (mode) {
            FeatureMode.XY -> base
            FeatureMode.X_ONLY -> doubleArrayOf(base[0])
            FeatureMode.Y_ONLY -> doubleArrayOf(base[1])
            FeatureMode.SUM -> doubleArrayOf(base[0], base[1], base[0] + base[1])
        }
    }

    fun dataset(curve: ToyCurve, mode: FeatureMode): List<EvalSample> {
        val order = curve.generatorOrder.toDouble()
        return curve.allKeyPairs().map { pair ->
            EvalSample(pair.privateKey, featuresFor(curve, pair.publicKey, mode), pair.privateKey / order)
        }
    }

    fun kFoldExactRates(curve: ToyCurve, hiddenUnits: Int, epochs: Int, folds: Int = 3, seed: Long = 42L): List<Double> {
        val samples = dataset(curve, FeatureMode.XY)
        require(folds in 2..samples.size) { "Invalid fold count" }
        val inputSize = samples.first().features.size
        val rates = mutableListOf<Double>()
        for (fold in 0 until folds) {
            val holdout = samples.filterIndexed { index, _ -> index % folds == fold }
            val train = samples.filterIndexed { index, _ -> index % folds != fold }
            val model = TinyMLP(inputSize, hiddenUnits, seed + fold)
            model.train(train.map { NeuralSample(it.features, it.target) }, NeuralConfig(hiddenUnits, epochs, 0.08, seed + fold, 5), curve.generatorOrder)
            val exact = holdout.count { sample ->
                TinyMLP.estimatePrivateKey(model.predict(sample.features), curve.generatorOrder) == sample.key
            }
            rates.add(exact.toDouble() / holdout.size)
        }
        return rates
    }

    fun ablations(curve: ToyCurve, hiddenUnits: Int, epochs: Int, seed: Long = 42L): Map<FeatureMode, Double> {
        return FeatureMode.entries.associateWith { mode ->
            val samples = dataset(curve, mode)
            val model = TinyMLP(samples.first().features.size, hiddenUnits, seed + mode.ordinal)
            val result = model.train(samples.map { NeuralSample(it.features, it.target) }, NeuralConfig(hiddenUnits, epochs, 0.08, seed + mode.ordinal, 5), curve.generatorOrder)
            result.validationExactRate
        }
    }

    fun confusion(curve: ToyCurve, hiddenUnits: Int, epochs: Int, seed: Long = 42L): ConfusionReport {
        val samples = dataset(curve, FeatureMode.XY)
        val model = TinyMLP(samples.first().features.size, hiddenUnits, seed + 100)
        model.train(samples.map { NeuralSample(it.features, it.target) }, NeuralConfig(hiddenUnits, epochs, 0.08, seed + 100, 5), curve.generatorOrder)
        val counts = HashMap<Pair<Long, Long>, Int>()
        var exact = 0
        for (sample in samples) {
            val estimated = TinyMLP.estimatePrivateKey(model.predict(sample.features), curve.generatorOrder)
            if (estimated == sample.key) exact++ else counts.merge(sample.key to estimated, 1, Int::plus)
        }
        val top = counts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (pair, count) -> ConfusionPair(pair.first, pair.second, count) }
        return ConfusionReport(exact.toDouble() / samples.size, samples.size, top)
    }

    fun evaluate(curve: ToyCurve, hiddenUnits: Int, epochs: Int, folds: Int = 3, seed: Long = 42L): NeuralEvalReport {
        return NeuralEvalReport(
            kFoldExactRates = kFoldExactRates(curve, hiddenUnits, epochs, folds, seed),
            ablations = ablations(curve, hiddenUnits, epochs, seed),
            confusion = confusion(curve, hiddenUnits, epochs, seed)
        )
    }
}
