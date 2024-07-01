package com.apollographql.apollo.adapter

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import platform.Foundation.NSDecimalNumber

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
actual class BigDecimal internal constructor(private val raw: NSDecimalNumber) : Number() {

  actual constructor(strVal: String) : this(NSDecimalNumber(strVal))

  actual constructor(doubleVal: Double) : this(NSDecimalNumber(doubleVal))

  actual constructor(intVal: Int) : this(NSDecimalNumber(int = intVal))

  actual constructor(longVal: Long) : this(NSDecimalNumber(longLong = longVal))

  actual fun add(augend: BigDecimal): BigDecimal = BigDecimal(raw.decimalNumberByAdding(augend.raw))

  actual fun subtract(subtrahend: BigDecimal): BigDecimal = BigDecimal(raw.decimalNumberBySubtracting(subtrahend.raw))

  actual fun multiply(multiplicand: BigDecimal): BigDecimal = BigDecimal(raw.decimalNumberByMultiplyingBy(multiplicand.raw))

  actual fun divide(divisor: BigDecimal): BigDecimal = BigDecimal(raw.decimalNumberByDividingBy(divisor.raw))

  actual fun negate(): BigDecimal = BigDecimal(NSDecimalNumber(int = 0).decimalNumberBySubtracting(raw))

  actual fun signum(): Int {
    val result = raw.compare(NSDecimalNumber(int = 0)).toInt()
    return when {
      result < 0 -> -1
      result > 0 -> 1
      else -> 0
    }
  }

  override fun toInt(): Int {
    return raw.intValue
  }

  override fun toLong(): Long {
    return raw.longLongValue
  }

  override fun toShort(): Short {
    return raw.shortValue
  }

  override fun toByte(): Byte {
    return raw.charValue
  }

  override fun toDouble(): Double {
    return raw.doubleValue
  }

  override fun toFloat(): Float {
    return raw.floatValue
  }

  override fun equals(other: Any?): Boolean {
    return (this === other) || raw == (other as? BigDecimal)?.raw
  }

  override fun hashCode(): Int = raw.hashCode()

  override fun toString(): String = raw.stringValue
}

actual fun BigDecimal.toNumber(): Number = this
