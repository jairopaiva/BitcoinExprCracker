package com.jairopaiva.exprlab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LabsScreen() {
    var selected by remember { mutableStateOf("Curve") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = selected == "Curve", onClick = { selected = "Curve" }, label = { Text("Toy curve") })
            FilterChip(selected = selected == "ECDLP", onClick = { selected = "ECDLP" }, label = { Text("ECDLP") })
            FilterChip(selected = selected == "Neural", onClick = { selected = "Neural" }, label = { Text("Neural toy") })
            FilterChip(selected = selected == "NN eval", onClick = { selected = "NN eval" }, label = { Text("NN eval") })
            FilterChip(selected = selected == "Genetic", onClick = { selected = "Genetic" }, label = { Text("Genetic") })
            FilterChip(selected = selected == "Side-channel", onClick = { selected = "Side-channel" }, label = { Text("Side-channel") })
        }
        when (selected) {
            "Curve" -> ToyCurveScreen()
            "ECDLP" -> EcdlpVisualizerScreen()
            "Neural" -> NeuralLabScreen()
            "NN eval" -> NeuralEvalScreen()
            "Genetic" -> GeneticLabScreen()
            else -> SideChannelScreen()
        }
    }
}
