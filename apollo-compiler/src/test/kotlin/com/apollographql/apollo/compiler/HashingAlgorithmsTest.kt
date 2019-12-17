package com.apollographql.apollo.compiler

import org.junit.Assert.*
import org.junit.Test

class HashingAlgorithmsTest {

    @Test
    fun testDefaulNulltHashing() {
        val source = "This is a test message"
        val expected = "6f3438001129a90c5b1637928bf38bf26e39e57c6e9511005682048bedbef906"
        assertEquals("Hashing doesn't work if not specified in gradle file!",
                HashingAlgorithms(null).applyHashing(source),
                expected)
    }

    @Test
    fun testDefaulInvalidtHashing() {
        val source = "This is a test message"
        val expected = "6f3438001129a90c5b1637928bf38bf26e39e57c6e9511005682048bedbef906"
        assertEquals("Hashing doesn't work if specified hash in gradle file is invalid!",
                HashingAlgorithms("").applyHashing(source),
                expected)
    }

    @Test
    fun testSha256tHashing() {
        val source = "This is a test message"
        val expected = "6f3438001129a90c5b1637928bf38bf26e39e57c6e9511005682048bedbef906"
        assertEquals("SHA256 Hashing doesn't work!",
                HashingAlgorithms("SHA256").applyHashing(source),
                expected)
    }

    @Test
    fun testMd5tHashing() {
        val source = "This is a test message"
        val expected = "fafb00f5732ab283681e124bf8747ed1"
        assertEquals("MD5 Hashing doesn't work!",
                HashingAlgorithms("MD5").applyHashing(source),
                expected)
    }

}