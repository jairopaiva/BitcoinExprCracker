package com.jairopaiva.exprlab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jairopaiva.exprlab.domain.EcdlpLab
import com.jairopaiva.exprlab.domain.EcdlpResult
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
fun EcdlpVisualizerScreen() {
    val curve = remember { ToyCurve() }
    var key by remember { mutableLongStateOf(7L) }
    val pair = remember(key) { curve.keyPair(key) }
    var result by remember { mutableStateOf<EcdlpResult?>(null) }
    var method by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val complexity = remember { EcdlpLab.complexityRows() }

    fun run(block: suspend () -> EcdlpResult, label: String) {
        scope.launch {
            isLoading = true
            error = null
            try {
                result = withContext(Dispatchers.Default) { block() }
                method = label
            } catch (exception: Exception) {
                error = exception.message ?: exception.javaClass.simpleName
            } finally {
                isLoading = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoCard(text = "Toy-curve ECDLP visualizer. Pollard rho and kangaroo are shown only on the order-29 toy group; secp256k1 remains computationally infeasible.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryAction(text = "Pollard rho", onClick = { run({ EcdlpLab.pollardRho(curve, pair.publicKey) }, "Pollard rho") }, enabled = !isLoading)
            PrimaryAction(text = "Kangaroo", onClick = { run({ EcdlpLab.kangaroo(curve, pair.publicKey) }, "Kangaroo") }, enabled = !isLoading)
            PrimaryAction(text = "New target", onClick = { key = key % 28 + 1; result = null }, enabled = !isLoading)
        }
        if (isLoading) Text("Solving toy ECDLP...")
        error?.let { ErrorCard(message = it) }
        result?.let {
            SectionCard(title = "Recovered toy key") {
                MetricRow("Method", method)
                MetricRow("Private key", it.privateKey.toString())
                MetricRow("Match", if (it.privateKey == key) "yes" else "no")
                MetricRow("Steps", it.steps.toString())
                MetricRow("Elapsed", "${it.elapsedMs} ms")
            }
        }
        SectionCard(title = "Why secp256k1 is different") {
            complexity.forEach { row ->
                MetricRow(row.label, "rho ${row.expectedRhoSteps} · kangaroo ${row.expectedKangarooSteps} · device ${if (row.feasibleOnDevice) "toy only" else "infeasible"}")
            }
        }
        WarningBanner(text = "This is an education demo. It does not recover Bitcoin private keys and does not claim neural networks solve ECDLP.")
    }
}
