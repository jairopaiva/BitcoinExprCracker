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
import com.jairopaiva.exprlab.domain.SideChannelReport
import com.jairopaiva.exprlab.domain.ToyCurve
import com.jairopaiva.exprlab.domain.ToySideChannel
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
fun SideChannelScreen() {
    val curve = remember { ToyCurve() }
    var report by remember { mutableStateOf<SideChannelReport?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoCard(text = "Toy timing demo: variable-time double-and-add versus a fixed-work toy loop on the order-29 curve. Educational only.")
        PrimaryAction(
            text = "Run timing demo",
            onClick = {
                scope.launch {
                    isLoading = true
                    error = null
                    try {
                        report = withContext(Dispatchers.Default) {
                            ToySideChannel.report(curve, ToySideChannel.defaultKeys(curve), repetitions = 120)
                        }
                    } catch (exception: Exception) {
                        error = exception.message ?: exception.javaClass.simpleName
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        )
        if (isLoading) Text("Measuring toy timings...")
        error?.let { ErrorCard(message = it) }
        report?.let {
            SectionCard(title = "Timing summary") {
                MetricRow("Average variable", "${it.averageVariableNs.toLong()} ns")
                MetricRow("Average constant", "${it.averageConstantNs.toLong()} ns")
                MetricRow("Variable spread", "${it.variableSpreadNs} ns")
                MetricRow("Constant spread", "${it.constantSpreadNs} ns")
                it.samples.take(8).forEach { sample -> MetricRow("key ${sample.key}", "var ${sample.variableNs} ns · const ${sample.constantNs} ns") }
            }
        }
        WarningBanner(text = "This demonstrates the concept on toy arithmetic. It is not constant-time crypto and says nothing about audited secp256k1 libraries.")
    }
}
