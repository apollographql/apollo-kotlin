@file:Suppress("DEPRECATION")

package com.apollographql.apollo.adapter.test

import com.apollographql.apollo.adapter.BigDecimal
import kotlin.test.*

class BigDecimalTests {
  @Test
  fun equality() {
    assertEquals(BigDecimal("12345.12345678901234567890123"), BigDecimal("12345.12345678901234567890123"))
    assertEquals(BigDecimal("987654321098765432109876543210"), BigDecimal("987654321098765432109876543210"))
    assertEquals(BigDecimal("1024768"), BigDecimal("1024768"))
    assertEquals(BigDecimal(-Double.MAX_VALUE), BigDecimal(-Double.MAX_VALUE))
    assertEquals(BigDecimal(Double.MAX_VALUE), BigDecimal(Double.MAX_VALUE))
    assertEquals(BigDecimal(-Long.MAX_VALUE - 1), BigDecimal(-Long.MAX_VALUE - 1))
    assertEquals(BigDecimal(Long.MAX_VALUE), BigDecimal(Long.MAX_VALUE))
  }

  @Test
  fun overflow() {
    assertFails {
      BigDecimal("987654321098765432109876543210").toLong()
    }
  }

  @Test
  fun truncating() {

    assertEquals(
      BigDecimal("12345.12345678901234567890123").toDouble(),
        12345.123456789011
    )
  }

  @Test
  fun roundTrip_Int() {
    val bridged = BigDecimal(Int.MAX_VALUE)
    assertEquals(bridged.toInt(), Int.MAX_VALUE)

    val bridgedNeg = BigDecimal( -Int.MAX_VALUE - 1)
    assertEquals(bridgedNeg.toInt(), -Int.MAX_VALUE - 1)
  }

  @Test
  fun roundTrip_Long() {
    val bridged = BigDecimal(Long.MAX_VALUE)
    assertEquals(bridged.toLong(), Long.MAX_VALUE)

    val bridgedNeg = BigDecimal(-Long.MAX_VALUE - 1)
    assertEquals(bridgedNeg.toLong(), -Long.MAX_VALUE - 1)
  }

  @Test
  fun roundTrip_Double() {
    val bridged = BigDecimal(Double.MAX_VALUE)
    assertEquals(bridged.toDouble(), Double.MAX_VALUE)

    val bridgedNeg = BigDecimal(-Double.MAX_VALUE)
    assertEquals(bridgedNeg.toDouble(), -Double.MAX_VALUE)

    assertFails {
      BigDecimal(Double.POSITIVE_INFINITY)
    }

    assertFails {
      BigDecimal(Double.NEGATIVE_INFINITY)
    }

    assertFails {
      BigDecimal(Double.NaN)
    }
  }
}
