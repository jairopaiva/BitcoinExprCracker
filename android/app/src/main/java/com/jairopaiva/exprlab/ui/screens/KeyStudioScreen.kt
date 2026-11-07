package com.jairopaiva.exprlab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.jairopaiva.exprlab.domain.BitcoinCodec
import com.jairopaiva.exprlab.domain.Hex
import com.jairopaiva.exprlab.domain.Secp256k1
import com.jairopaiva.exprlab.domain.Secp256k1Point
import com.jairopaiva.exprlab.ui.components.CopyableText
import com.jairopaiva.exprlab.ui.components.PrimaryAction
import com.jairopaiva.exprlab.ui.components.SectionCard
import com.jairopaiva.exprlab.ui.components.WarningBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class KeyMaterial(
    val privateKey: ByteArray?,
    val point: Secp256k1Point
)

@Composable
fun KeyStudioScreen() {
    var privateKeyInput by remember { mutableStateOf("") }
    var publicKeyInput by remember { mutableStateOf("") }
    var testnet by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var material by remember { mutableStateOf<KeyMaterial?>(null) }
    val scope = rememberCoroutineScope()

    fun runJob(label: String, block: () -> KeyMaterial) {
        if (busy) return
        scope.launch {
            busy = true
            error = null
            try {
                val result = withContext(Dispatchers.Default) { block() }
                material = result
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
        SectionCard(title = "Ephemeral key studio") {
            Text(
                "Keys are generated locally with SecureRandom and kept only in memory. Use testnet/regtest for experiments.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !testnet, onClick = { testnet = false }, label = { Text("Mainnet") })
                FilterChip(selected = testnet, onClick = { testnet = true }, label = { Text("Testnet") })
            }
        }

        WarningBanner("Do not use generated keys for real funds. This screen is for research and testing only.")

        SectionCard(title = "Private key") {
            OutlinedTextField(
                value = privateKeyInput,
                onValueChange = { privateKeyInput = it },
                label = { Text("Private key hex (64 chars)") },
                singleLine = false,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryAction("Generate", enabled = !busy) {
                    runJob("Generate") {
                        val privateKey = Secp256k1.randomPrivateKey()
                        KeyMaterial(privateKey, Secp256k1.publicKeyFromPrivateBytes(privateKey))
                    }
                }
                PrimaryAction("Derive", enabled = !busy) {
                    runJob("Derive") {
                        val privateKey = Hex.decodeOrThrow(privateKeyInput, "private key")
                        KeyMaterial(privateKey, Secp256k1.publicKeyFromPrivateBytes(privateKey))
                    }
                }
            }
        }

        SectionCard(title = "Public key") {
            OutlinedTextField(
                value = publicKeyInput,
                onValueChange = { publicKeyInput = it },
                label = { Text("Public key hex (compressed or uncompressed)") },
                singleLine = false,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryAction("Load public key", enabled = !busy) {
                runJob("Load public key") {
                    val publicKey = Hex.decodeOrThrow(publicKeyInput, "public key")
                    val point = Secp256k1.decompress(publicKey) ?: error("Invalid public key")
                    KeyMaterial(null, point)
                }
            }
        }

        error?.let { WarningBanner(it) }
        if (busy) Text("Working…", color = MaterialTheme.colorScheme.onSurfaceVariant)

        material?.let { key ->
            val compressed = Secp256k1.compressed(key.point)
            val uncompressed = Secp256k1.uncompressed(key.point)
            SectionCard(title = "Key material") {
                key.privateKey?.let { privateKey ->
                    CopyableText("Private key", Hex.encode(privateKey))
                    CopyableText("WIF (${if (testnet) "testnet" else "mainnet"})", Secp256k1.privateKeyToWif(privateKey, testnet = testnet))
                }
                CopyableText("Public key compressed", Hex.encode(compressed))
                CopyableText("Public key uncompressed", Hex.encode(uncompressed))
            }

            SectionCard(title = "Addresses") {
                CopyableText("P2PKH", BitcoinCodec.p2pkhAddress(compressed, testnet))
                CopyableText("P2WPKH", BitcoinCodec.p2wpkhAddress(compressed, testnet))
                Text(
                    "Address derivation is standard HASH160/Base58Check or Bech32. It does not reveal the private key.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
