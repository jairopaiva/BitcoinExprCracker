package com.jairopaiva.exprlab.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jairopaiva.exprlab.domain.ToyCurve
import com.jairopaiva.exprlab.ui.components.SectionCard
import com.jairopaiva.exprlab.ui.components.WarningBanner

@Composable
fun HomeScreen(
    onOpenKeys: () -> Unit,
    onOpenExpressions: () -> Unit,
    onOpenLabs: () -> Unit
) {
    val toyOrder = remember { ToyCurve().generatorOrder }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard(title = "ExprCracker Lab") {
            Text(
                "A lightweight Android research client for secp256k1 expressions, local key tooling, and toy cryptographic labs.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        WarningBanner(
            "Educational demo only. It does not recover real Bitcoin private keys from addresses or public keys. " +
                "The toy labs use a tiny curve (order $toyOrder) so brute force and a small neural network are feasible."
        )

        FeatureCard(
            title = "Key Studio",
            body = "Generate ephemeral secp256k1 keys locally, inspect compressed/uncompressed public keys, WIF, and P2PKH/P2WPKH addresses.",
            onClick = onOpenKeys
        )
        FeatureCard(
            title = "Expression Lab",
            body = "Port of the pseudo-random expression generator over public-key bytes, with an explicit self-test mode that requires the private key.",
            onClick = onOpenExpressions
        )
        FeatureCard(
            title = "Toy Curve & Neural Lab",
            body = "Break a tiny elliptic curve by brute force and train a small neural network on toy public-key points to visualize why secp256k1 is different.",
            onClick = onOpenLabs
        )

        SectionCard(title = "Security boundary") {
            Text(
                "Secp256k1 public-key → private-key recovery is the elliptic-curve discrete-log problem. " +
                    "No expression search or neural network in this app can solve it for real Bitcoin keys. " +
                    "Private keys are kept in memory only and are never transmitted.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureCard(title: String, body: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
