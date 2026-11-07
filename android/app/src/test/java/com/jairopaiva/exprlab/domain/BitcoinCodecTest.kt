package com.jairopaiva.exprlab.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BitcoinCodecTest {
    @Test
    fun hash160HasExpectedLength() {
        assertEquals(20, BitcoinCodec.hash160(byteArrayOf(1, 2, 3)).size)
    }

    @Test
    fun p2wpkhAddressHasBech32Shape() {
        val compressed = ByteArray(33).also {
            it[0] = 0x02
            it[1] = 0x01
        }
        val address = BitcoinCodec.p2wpkhAddress(compressed, testnet = false)
        assertTrue(address.startsWith("bc1q"))
        assertEquals(42, address.length)
    }

    @Test
    fun p2pkhAddressHasBase58CheckShape() {
        val compressed = ByteArray(33).also {
            it[0] = 0x02
            it[1] = 0x01
        }
        val address = BitcoinCodec.p2pkhAddress(compressed, testnet = false)
        assertTrue(address.length in 25..35)
        assertTrue(address.all { char -> char in "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz" })
    }

    @Test
    fun bech32DecodeAcceptsBip173VectorAndRejectsChecksumMutation() {
        val valid = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
        val decoded = BitcoinCodec.bech32Decode(valid)
        assertTrue(decoded != null)
        assertEquals("bc", decoded!!.first)
        assertEquals(0, decoded.second)
        assertEquals(20, decoded.third.size)
        assertTrue(BitcoinCodec.isValidP2wpkhAddress(valid))
        assertFalse(BitcoinCodec.isValidP2wpkhAddress(valid.dropLast(1) + "5"))
    }

    @Test
    fun p2pkhAddressRoundTripsAndRejectsWrongNetworkOrChecksum() {
        val privateKey = ByteArray(32).also { it[31] = 1 }
        val compressed = Secp256k1.compressed(Secp256k1.publicKeyFromPrivateBytes(privateKey))
        val address = BitcoinCodec.p2pkhAddress(compressed)
        val decoded = BitcoinCodec.decodeP2pkhAddress(address)
        assertTrue(decoded != null)
        assertTrue(decoded!!.contentEquals(BitcoinCodec.hash160(compressed)))
        assertTrue(BitcoinCodec.isValidP2pkhAddress(address))
        assertFalse(BitcoinCodec.isValidP2pkhAddress(address, testnet = true))
        val tampered = address.dropLast(1) + if (address.last() == '1') "2" else "1"
        assertFalse(BitcoinCodec.isValidP2pkhAddress(tampered))
    }

    @Test
    fun p2wpkhAddressRoundTripsAndRejectsWrongNetworkOrChecksum() {
        val privateKey = ByteArray(32).also { it[31] = 1 }
        val compressed = Secp256k1.compressed(Secp256k1.publicKeyFromPrivateBytes(privateKey))
        val address = BitcoinCodec.p2wpkhAddress(compressed)
        val decoded = BitcoinCodec.decodeP2wpkhAddress(address)
        assertTrue(decoded != null)
        assertTrue(decoded!!.contentEquals(BitcoinCodec.hash160(compressed)))
        assertTrue(BitcoinCodec.isValidP2wpkhAddress(address))
        assertFalse(BitcoinCodec.isValidP2wpkhAddress(address, testnet = true))
        val tampered = address.dropLast(1) + if (address.last() == 'q') "p" else "q"
        assertFalse(BitcoinCodec.isValidP2wpkhAddress(tampered))
    }
}
