package com.jairopaiva.exprlab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.jairopaiva.exprlab.domain.ExpressionEngine
import com.jairopaiva.exprlab.domain.GeneratedExpression
import com.jairopaiva.exprlab.domain.Hex
import com.jairopaiva.exprlab.domain.Secp256k1
import com.jairopaiva.exprlab.domain.SelfTestResult
import com.jairopaiva.exprlab.ui.components.CopyableText
import com.jairopaiva.exprlab.ui.components.MetricRow
import com.jairopaiva.exprlab.ui.components.PrimaryAction
import com.jairopaiva.exprlab.ui.components.SectionCard
import com.jairopaiva.exprlab.ui.components.WarningBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExpressionLabScreen() {
    var publicKeyInput by remember { mutableStateOf("") }
    var privateKeyInput by remember { mutableStateOf("") }
    var seedInput by remember { mutableStateOf("1337") }
    var sampleCount by remember { mutableIntStateOf(8) }
    var sampleOperations by remember { mutableIntStateOf(4) }
    var samples by remember { mutableStateOf<List<GeneratedExpression>>(emptyList()) }
    var selfTest by remember { mutableStateOf<SelfTestResult?>(null) }
    var reconstructed by remember { mutableStateOf<ByteArray?>(null) }
    var progress by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun runJob(label: String, block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            progress = null
            try {
                block()
            } catch (exception: Exception) {
                error = "$label failed: ${exception.message ?: exception.javaClass.simpleName}"
            } finally {
                busy = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard(title = "Expression generator") {
            Text(
                "Generates deterministic byte expressions from public-key X/Y coordinates, inspired by the original C# project.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            WarningBanner("Self-test mode requires the private key. The app never derives a private key from a public key or address.")
        }

        SectionCard(title = "Inputs") {
            OutlinedTextField(
                value = publicKeyInput,
                onValueChange = { publicKeyInput = it },
                label = { Text("Public key hex") },
                singleLine = false,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = privateKeyInput,
                onValueChange = { privateKeyInput = it },
                label = { Text("Private key hex (self-test only, optional)") },
                singleLine = false,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = seedInput,
                onValueChange = { seedInput = it.filter { char -> char.isDigit() || char == '-' } },
                label = { Text("Seed") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Text("Samples: $sampleCount", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = sampleCount.toFloat(),
                onValueChange = { sampleCount = it.toInt().coerceIn(1, 32) },
                valueRange = 1f..32f
            )
            Text("Operations per expression: $sampleOperations", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = sampleOperations.toFloat(),
                onValueChange = { sampleOperations = it.toInt().coerceIn(1, 8) },
                valueRange = 1f..8f
            )
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryAction("Generate samples", enabled = !busy) {
                    runJob("Generate samples") {
                        val publicKey = Hex.decodeOrThrow(publicKeyInput, "public key")
                        val seed = seedInput.toLongOrNull() ?: 1337L
                        samples = withContext(Dispatchers.Default) {
                            ExpressionEngine.generateSamples(publicKey, seed, sampleCount, sampleOperations)
                        }
                    }
                }
                PrimaryAction("Self-test", enabled = !busy && privateKeyInput.isNotBlank()) {
                    runJob("Self-test") {
                        val privateKey = Hex.decodeOrThrow(privateKeyInput, "private key")
                        val publicKey = Hex.decodeOrThrow(publicKeyInput, "public key")
                        val result = withContext(Dispatchers.Default) {
                            ExpressionEngine.findTryes(privateKey, publicKey, maxTries = 50_000) { index, tries ->
                                progress = "Byte ${index + 1}/32 • $tries tries"
                            }
                        }
                        selfTest = result
                        if (result.completed) {
                            val (candidate, _) = withContext(Dispatchers.Default) {
                                ExpressionEngine.reconstructPrivateKey(publicKey, result.tryes)
                            }
                            reconstructed = candidate
                            progress = if (candidate.contentEquals(privateKey)) "Self-test passed" else "Self-test mismatch"
                        } else {
                            progress = "Self-test incomplete within try limit"
                        }
                    }
                }
                PrimaryAction("Use privkey=1", enabled = !busy) {
                    val privateKey = ByteArray(32).also { it[31] = 1 }
                    val point = Secp256k1.publicKeyFromPrivateBytes(privateKey)
                    privateKeyInput = Hex.encode(privateKey)
                    publicKeyInput = Hex.encode(Secp256k1.compressed(point))
                    samples = emptyList()
                    selfTest = null
                    reconstructed = null
                }
            }
            progress?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            error?.let { WarningBanner(it) }
        }

        samples.takeIf { it.isNotEmpty() }?.let { list ->
            SectionCard(title = "Generated expressions") {
                list.forEachIndexed { index, expression ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Candidate ${index + 1} • result 0x${expression.result.toString(16).padStart(2, '0')}",
                            fontWeight = FontWeight.SemiBold
                        )
                        SelectionContainer {
                            Text(expression.text, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        selfTest?.let { result ->
            SectionCard(title = "Self-test result") {
                MetricRow("Completed", if (result.completed) "yes" else "no")
                CopyableText("Tryes", result.tryes.joinToString(", "))
                reconstructed?.let { candidate ->
                    CopyableText("Reconstructed private key", Hex.encode(candidate))
                    Text(
                        if (candidate.contentEquals(Hex.decodeOrThrow(privateKeyInput, "private key"))) {
                            "Reconstruction matches the supplied private key."
                        } else {
                            "Reconstruction does not match the supplied private key."
                        },
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
