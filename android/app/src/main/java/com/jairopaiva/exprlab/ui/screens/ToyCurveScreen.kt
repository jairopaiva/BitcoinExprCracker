package com.jairopaiva.exprlab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jairopaiva.exprlab.domain.ToyCurve
import com.jairopaiva.exprlab.ui.components.MetricRow
import com.jairopaiva.exprlab.ui.components.PrimaryAction
import com.jairopaiva.exprlab.ui.components.SectionCard
import com.jairopaiva.exprlab.ui.components.WarningBanner
import kotlin.random.Random

@Composable
fun ToyCurveScreen() {
    val curve = remember { ToyCurve() }
    var challenge by remember { mutableStateOf(curve.keyPair(7L)) }
    var solvedKey by remember { mutableStateOf<Long?>(null) }
    var iterations by remember { mutableStateOf(0L) }
    var elapsedMs by remember { mutableStateOf(0L) }
    val points = remember(curve) { curve.points() }

    fun newChallenge() {
        val next = (1 until curve.generatorOrder).random(Random.Default).toLong()
        challenge = curve.keyPair(next)
        solvedKey = null
        iterations = 0L
        elapsedMs = 0L
    }

    fun solve() {
        val start = System.nanoTime()
        var count = 0L
        var found: Long? = null
        for (candidate in 1L until curve.generatorOrder.toLong()) {
            count = candidate
            if (curve.multiply(candidate, curve.generator) == challenge.publicKey) {
                found = candidate
                break
            }
        }
        iterations = count
        elapsedMs = (System.nanoTime() - start) / 1_000_000
        solvedKey = found
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard(title = "Toy elliptic curve") {
            Text("y² = x³ + x + 4 over F₂₃ • order ${curve.generatorOrder}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            WarningBanner("This is a deliberately tiny curve for education. It is unrelated to secp256k1 key security.")
            RowActions(
                onNew = ::newChallenge,
                onSolve = ::solve,
                solved = solvedKey != null
            )
        }

        SectionCard(title = "Curve plot") {
            ToyCurveCanvas(
                curve = curve,
                points = points,
                target = challenge.publicKey,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            )
        }

        SectionCard(title = "Brute-force result") {
            MetricRow("Generator order", curve.generatorOrder.toString())
            MetricRow("Iterations", iterations.toString())
            MetricRow("Elapsed", "${elapsedMs} ms")
            solvedKey?.let { key ->
                MetricRow("Recovered private key", key.toString())
                MetricRow("Matches challenge", if (key == challenge.privateKey) "yes" else "no")
            } ?: Text("Solve the challenge to recover the toy private key.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RowActions(onNew: () -> Unit, onSolve: () -> Unit, solved: Boolean) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryAction("New challenge", onClick = onNew)
        PrimaryAction(if (solved) "Solve again" else "Solve", onClick = onSolve)
    }
}

@Composable
private fun ToyCurveCanvas(
    curve: ToyCurve,
    points: List<com.jairopaiva.exprlab.domain.ToyPoint>,
    target: com.jairopaiva.exprlab.domain.ToyPoint,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val pad = 28.dp.toPx()
        val span = (curve.p - 1).toFloat()
        val scale = minOf((size.width - pad * 2) / span, (size.height - pad * 2) / span)
        val offsetX = (size.width - span * scale) / 2f
        val offsetY = (size.height - span * scale) / 2f

        fun xPixel(x: Long): Float = offsetX + x * scale
        fun yPixel(y: Long): Float = size.height - offsetY - y * scale

        val gridColor = Color(0xFF2A3550)
        for (i in 0 until curve.p) {
            val x = xPixel(i)
            val y = yPixel(i)
            drawLine(gridColor, Offset(x, pad), Offset(x, size.height - pad), strokeWidth = 1f)
            drawLine(gridColor, Offset(pad, y), Offset(size.width - pad, y), strokeWidth = 1f)
        }

        val pointRadius = 3.dp.toPx()
        for (point in points) {
            drawCircle(Color(0xFF8AB4FF).copy(alpha = 0.55f), pointRadius, Offset(xPixel(point.x), yPixel(point.y)))
        }
        drawCircle(Color(0xFF81C995), pointRadius * 1.4f, Offset(xPixel(curve.generator.x), yPixel(curve.generator.y)))
        drawCircle(Color(0xFFF8BBA0), pointRadius * 1.8f, Offset(xPixel(target.x), yPixel(target.y)))
    }
}
