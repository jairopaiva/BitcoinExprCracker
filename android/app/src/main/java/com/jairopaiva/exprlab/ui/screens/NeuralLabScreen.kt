package com.jairopaiva.exprlab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jairopaiva.exprlab.domain.NeuralConfig
import com.jairopaiva.exprlab.domain.NeuralSample
import com.jairopaiva.exprlab.domain.TinyMLP
import com.jairopaiva.exprlab.domain.TrainingResult
import com.jairopaiva.exprlab.domain.ToyCurve
import com.jairopaiva.exprlab.ui.components.MetricRow
import com.jairopaiva.exprlab.ui.components.PrimaryAction
import com.jairopaiva.exprlab.ui.components.SectionCard
import com.jairopaiva.exprlab.ui.components.WarningBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NeuralLabScreen() {
    val curve = remember { ToyCurve() }
    val samples = remember(curve) {
        curve.allKeyPairs().map { pair ->
            NeuralSample(
                features = curve.featuresFor(pair.publicKey),
                target = pair.privateKey.toDouble() / curve.generatorOrder
            )
        }
    }
    val inputSize = remember(samples) { samples.first().features.size }

    var hiddenUnits by remember { mutableIntStateOf(16) }
    var epochs by remember { mutableIntStateOf(600) }
    var targetKey by remember { mutableIntStateOf(7) }
    var model by remember { mutableStateOf<TinyMLP?>(null) }
    var result by remember { mutableStateOf<TrainingResult?>(null) }
    var prediction by remember { mutableStateOf<Double?>(null) }
    var estimatedKey by remember { mutableStateOf<Long?>(null) }
    var progress by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun train() {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            progress = null
            try {
                val config = NeuralConfig(
                    hiddenUnits = hiddenUnits,
                    epochs = epochs,
                    learningRate = 0.08,
                    seed = 42L,
                    validationEvery = 5
                )
                val mlp = TinyMLP(inputSize, hiddenUnits, config.seed)
                val trainingResult = withContext(Dispatchers.Default) {
                    mlp.train(samples, config, curve.generatorOrder) { point ->
                        if (point.epoch % 50 == 0 || point.epoch == config.epochs) {
                            progress = "Epoch ${point.epoch}/${config.epochs}"
                        }
                    }
                }
                model = mlp
                result = trainingResult
                prediction = null
                estimatedKey = null
                progress = "Training complete"
            } catch (exception: Exception) {
                error = "Training failed: ${exception.message ?: exception.javaClass.simpleName}"
            } finally {
                busy = false
            }
        }
    }

    fun predict() {
        val currentModel = model ?: return
        val pair = curve.keyPair(targetKey.toLong())
        val output = currentModel.predict(curve.featuresFor(pair.publicKey))
        prediction = output
        estimatedKey = TinyMLP.estimatePrivateKey(output, curve.generatorOrder)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard(title = "Tiny neural network") {
            Text(
                "A small MLP is trained on toy-curve public points. The target is the toy private key normalized by curve order ${curve.generatorOrder}.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            WarningBanner("Toy curve only. This experiment illustrates why a neural network does not break secp256k1.")
        }

        SectionCard(title = "Configuration") {
            Text("Hidden units: $hiddenUnits", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = hiddenUnits.toFloat(), onValueChange = { hiddenUnits = it.toInt().coerceIn(4, 32) }, valueRange = 4f..32f)
            Text("Epochs: $epochs", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = epochs.toFloat(), onValueChange = { epochs = it.toInt().coerceIn(50, 1500) }, valueRange = 50f..1500f)
            Text("Target toy private key: $targetKey", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = targetKey.toFloat(), onValueChange = { targetKey = it.toInt().coerceIn(1, curve.generatorOrder - 1) }, valueRange = 1f..(curve.generatorOrder - 1).toFloat())
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryAction("Train model", enabled = !busy, onClick = ::train)
                PrimaryAction("Predict", enabled = model != null && !busy, onClick = ::predict)
            }
            progress?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            error?.let { WarningBanner(it) }
        }

        result?.let { training ->
            SectionCard(title = "Training metrics") {
                MetricRow("Train loss", "%.6f".format(training.finalTrainLoss))
                MetricRow("Validation loss", "%.6f".format(training.finalValidationLoss))
                MetricRow("Validation exact rate", "%.1f%%".format(training.validationExactRate * 100))
                MetricRow("Validation samples", training.validationCount.toString())
                LossChart(history = training.history, modifier = Modifier.fillMaxWidth().height(180.dp))
            }
        }

        if (prediction != null && estimatedKey != null) {
            SectionCard(title = "Prediction") {
                MetricRow("Normalized output", "%.6f".format(prediction!!))
                MetricRow("Estimated toy key", estimatedKey.toString())
                MetricRow("True toy key", targetKey.toString())
                MetricRow("Exact", if (estimatedKey == targetKey.toLong()) "yes" else "no")
                Text(
                    "Even on a 29-order toy curve, exact recovery depends on memorization/generalization; secp256k1 has ~2^256 possibilities.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LossChart(history: List<com.jairopaiva.exprlab.domain.LossPoint>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (history.isEmpty()) return@Canvas
        val pad = 24.dp.toPx()
        val width = size.width - pad * 2
        val height = size.height - pad * 2
        val maxLoss = history.maxOf { maxOf(it.trainLoss, it.validationLoss) }.coerceAtLeast(1e-9)
        val minLoss = history.minOf { minOf(it.trainLoss, it.validationLoss) }.coerceAtLeast(0.0)
        val span = (maxLoss - minLoss).coerceAtLeast(1e-9)
        val epochs = history.size

        fun point(index: Int, loss: Double): Offset {
            val x = pad + (index.toFloat() / (epochs - 1).coerceAtLeast(1)) * width
            val y = pad + height - ((loss - minLoss) / span).toFloat() * height
            return Offset(x, y)
        }

        val trainPath = Path()
        val validationPath = Path()
        history.forEachIndexed { index, loss ->
            val train = point(index, loss.trainLoss)
            val validation = point(index, loss.validationLoss)
            if (index == 0) {
                trainPath.moveTo(train.x, train.y)
                validationPath.moveTo(validation.x, validation.y)
            } else {
                trainPath.lineTo(train.x, train.y)
                validationPath.lineTo(validation.x, validation.y)
            }
        }
        drawPath(trainPath, Color(0xFF8AB4FF), style = Stroke(width = 3.dp.toPx()))
        drawPath(validationPath, Color(0xFF81C995), style = Stroke(width = 3.dp.toPx()))
    }
}
