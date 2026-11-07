package com.jairopaiva.exprlab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jairopaiva.exprlab.domain.NeuralEvaluation
import com.jairopaiva.exprlab.domain.NeuralEvalReport
import com.jairopaiva.exprlab.domain.ToyCurve
import com.jairopaiva.exprlab.ui.components.ErrorCard
import com.jairopaiva.exprlab.ui.components.InfoCard
import com.jairopaiva.exprlab.ui.components.MetricRow
import com.jairopaiva.exprlab.ui.components.PrimaryAction
import com.jairopaiva.exprlab.ui.components.SectionCard
import com.jairopaiva.exprlab.ui.components.WarningBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NeuralEvalScreen() {
    val curve = remember { ToyCurve() }
    var report by remember { mutableStateOf<NeuralEvalReport?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoCard(text = "Generalization dashboard for the toy MLP: k-fold exact rates, feature ablations, and confusion pairs. Toy curve only.")
        PrimaryAction(
            text = "Run evaluation",
            onClick = {
                scope.launch {
                    isLoading = true
                    error = null
                    try {
                        report = withContext(Dispatchers.Default) { NeuralEvaluation.evaluate(curve, hiddenUnits = 8, epochs = 30, folds = 2, seed = 7L) }
                    } catch (exception: Exception) {
                        error = exception.message ?: exception.javaClass.simpleName
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        )
        if (isLoading) Text("Evaluating toy neural network...")
        error?.let { ErrorCard(message = it) }
        report?.let { value ->
            SectionCard(title = "K-fold exact rates") {
                value.kFoldExactRates.forEachIndexed { index, rate -> MetricRow("Fold ${index + 1}", "${(rate * 100).toInt()}%") }
            }
            SectionCard(title = "Feature ablations") {
                value.ablations.forEach { (mode, rate) -> MetricRow(mode.label, "${(rate * 100).toInt()}%") }
            }
            SectionCard(title = "Confusion") {
                MetricRow("Exact rate", "${(value.confusion.exactRate * 100).toInt()}%")
                MetricRow("Samples", value.confusion.total.toString())
                value.confusion.topConfusions.forEach { pair -> MetricRow("true ${pair.trueKey} -> est ${pair.estimatedKey}", pair.count.toString()) }
            }
        }
        WarningBanner(text = "These metrics measure a tiny toy model on a 28-key group. They do not imply any capability against secp256k1.")
    }
}
