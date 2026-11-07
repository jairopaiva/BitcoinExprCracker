package com.jairopaiva.exprlab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selected == "Curve", onClick = { selected = "Curve" }, label = { Text("Toy curve") })
            FilterChip(selected = selected == "Neural", onClick = { selected = "Neural" }, label = { Text("Neural toy") })
        }
        if (selected == "Curve") ToyCurveScreen() else NeuralLabScreen()
    }
}
