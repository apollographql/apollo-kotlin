package com.apollographql.apollo.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BigDecimalTest {

  @Test
  fun add() {
    assertEquals(2.0, BigDecimal(1).add(BigDecimal(1)).toDouble())
    assertEquals(4.25, BigDecimal(2.5).add(BigDecimal(1.75)).toDouble())
  }

  @Test
  fun subtract() {
    assertEquals(0.0, BigDecimal(1).subtract(BigDecimal(1)).toDouble())
    assertEquals(0.75, BigDecimal(2.5).subtract(BigDecimal(1.75)).toDouble())
  }

  @Test
  fun multiply() {
    assertEquals(1.0, BigDecimal(1).multiply(BigDecimal(1)).toDouble())
    assertEquals(4.375, BigDecimal(2.5).multiply(BigDecimal(1.75)).toDouble())
  }

  @Test
  fun divide() {
    assertEquals(2.0, BigDecimal(4.0).divide(BigDecimal(2.0)).toDouble())
    assertEquals(2.5, BigDecimal(5.0).divide(BigDecimal(2.0)).toDouble())
  }

  @Test
  fun negate() {
    assertEquals(BigDecimal(-1), BigDecimal(1).negate())
    assertEquals(BigDecimal(1), BigDecimal(-1).negate())
  }

  @Test
  fun signum() {
    assertEquals(0, BigDecimal(0).signum())
    assertEquals(-1, BigDecimal(-1).signum())
    assertEquals(-1, BigDecimal(-1).signum())
  }

  // From kotlin.Number

  @Test
  fun toInt() {
    assertEquals(1, BigDecimal(1).toInt())
    assertEquals(-1, BigDecimal(-1).toInt())
  }

  @Test
  fun toLong() {
    assertEquals(1L, BigDecimal(1).toLong())
    assertEquals(-1L, BigDecimal(-1).toLong())
  }

  @Test
  fun toDouble() {
    assertEquals(1.0, BigDecimal(1.0).toDouble())
    assertEquals(-1.0, BigDecimal(-1.0).toDouble())
  }

  @Test
  fun is_a_Number() {
    // Fails in JS without the patch in `Kotlin.isNumber`
    assertTrue { BigDecimal(1) is Number }
  }
}