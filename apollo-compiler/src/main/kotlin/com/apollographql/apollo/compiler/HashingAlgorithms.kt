package com.apollographql.apollo.compiler

import java.math.BigInteger
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

class HashingAlgorithms(
    private val algorithm: String?
) {

    fun applyHashing(text: String): String {
        return when ((algorithm ?: SHA256).toUpperCase(Locale.ENGLISH)) {
            SHA256 -> sha256(text)
            MD5 -> md5(text)
            else -> sha256(text)
        }
    }

    private fun sha256(text: String): String {
        val bytes = text.toByteArray(charset = StandardCharsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun md5(text: String): String {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(text.toByteArray(Charset.forName("US-ASCII")), 0, text.length)
        val magnitude = digest.digest()
        return String.format("%0" + (magnitude.size shl 1) + "x", BigInteger(1, magnitude))
    }

    companion object {
        const val SHA256 = "SHA256"
        const val MD5 = "MD5"
    }

}