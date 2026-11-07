package com.jairopaiva.exprlab.domain

import org.junit.Assert.assertEquals
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
}
