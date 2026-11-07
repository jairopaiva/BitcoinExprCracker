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
import com.jairopaiva.exprlab.domain.GeneticResult
import com.jairopaiva.exprlab.domain.GeneticExpressions
import com.jairopaiva.exprlab.domain.Secp256k1
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
fun GeneticLabScreen() {
    val privateKey1 = remember { ByteArray(32).also { it[31] = 1 } }
    var targets by remember { mutableStateOf<ByteArray?>(null) }
    var publicKey by remember { mutableStateOf<ByteArray?>(null) }
    var result by remember { mutableStateOf<GeneticResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun prepare() {
        targets = privateKey1.copyOfRange(28, 32)
        publicKey = Secp256k1.compressed(Secp256k1.publicKeyFromPrivateBytes(privateKey1))
        result = null
    }

    fun run() {
        val targetBytes = targets ?: return
        val publicBytes = publicKey ?: return
        scope.launch {
            isLoading = true
            error = null
            try {
                result = withContext(Dispatchers.Default) {
                    GeneticExpressions.run(targetBytes, publicBytes, populationSize = 32, generations = 10, seed = 5L)
                }
            } catch (exception: Exception) {
                error = exception.message ?: exception.javaClass.simpleName
            } finally {
                isLoading = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoCard(text = "AST genetic-programming toy. It evolves byte expressions against one known public key; it cannot create information from a public key alone.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryAction(text = "Use privkey=1", onClick = ::prepare, enabled = !isLoading)
            PrimaryAction(text = "Run GP", onClick = ::run, enabled = !isLoading && targets != null && publicKey != null)
        }
        if (isLoading) Text("Evolving expressions...")
        error?.let { ErrorCard(message = it) }
        result?.let {
            SectionCard(title = "Best individual") {
                MetricRow("Fitness", it.bestFitness.toString())
                MetricRow("Targets", it.targetCount.toString())
                MetricRow("First fitness", it.generations.firstOrNull()?.toString() ?: "-")
                MetricRow("Last fitness", it.generations.lastOrNull()?.toString() ?: "-")
                it.bestExpressions.forEachIndexed { index, expression -> MetricRow("Expr $index", expression) }
            }
        }
        WarningBanner(text = "Fitness improvements on a toy target do not imply private-key recovery for real Bitcoin keys.")
    }
}
