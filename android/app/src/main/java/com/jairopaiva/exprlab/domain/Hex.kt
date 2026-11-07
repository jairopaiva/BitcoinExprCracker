package com.jairopaiva.exprlab.domain

object Hex {
    private const val HEX_DIGITS = "0123456789abcdef"

    fun encode(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val value = byte.toInt() and 0xff
            out.append(HEX_DIGITS[value ushr 4])
            out.append(HEX_DIGITS[value and 0x0f])
        }
        return out.toString()
    }

    fun decode(text: String): ByteArray? {
        val clean = text.filterNot { it.isWhitespace() }
        if (clean.isEmpty() || clean.length % 2 != 0) return null
        val out = ByteArray(clean.length / 2)
        var index = 0
        var high = -1
        for (char in clean) {
            val value = char.digitToIntOrNull(16) ?: return null
            if (high < 0) {
                high = value
            } else {
                out[index++] = ((high shl 4) or value).toByte()
                high = -1
            }
        }
        return out
    }

    fun decodeOrThrow(text: String, label: String = "hex"): ByteArray {
        return decode(text) ?: throw IllegalArgumentException("Invalid $label hex")
    }

    fun ByteArray.toHex(): String = encode(this)
}
