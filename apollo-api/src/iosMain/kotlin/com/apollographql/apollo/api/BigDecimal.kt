package com.apollographql.apollo.api

import platform.Foundation.NSDecimalNumber

actual class BigDecimal internal constructor(private val raw: NSDecimalNumber) : Number(), Comparable<BigDecimal> {

  actual constructor(strVal: String) : this(NSDecimalNumber(strVal))

  actual constructor(doubleVal: Double) : this(NSDecimalNumber(doubleVal))

  actual constructor(intVal: Int) : this(NSDecimalNumber(intVal))

  actual constructor(longVal: Long) : this(NSDecimalNumber(long = longVal))

  actual fun add(augend: BigDecimal): BigDecimal = BigDecimal(raw.decimalNumberByAdding(augend.raw))

  actual fun subtract(subtrahend: BigDecimal): BigDecimal = BigDecimal(raw.decimalNumberBySubtracting(subtrahend.raw))

  actual fun multiply(multiplicand: BigDecimal): BigDecimal = BigDecimal(raw.decimalNumberByMultiplyingBy(multiplicand.raw))

  actual fun divide(divisor: BigDecimal): BigDecimal = BigDecimal(raw.decimalNumberByDividingBy(divisor.raw))

  actual fun negate(): BigDecimal = BigDecimal(NSDecimalNumber(0).decimalNumberBySubtracting(raw))

  actual fun signum(): Int {
    val result = raw.compare(NSDecimalNumber(0)).toInt()
    return when {
      result < 0 -> -1
      result > 0 -> 1
      else -> 0
    }
  }

  override fun compareTo(other: BigDecimal): Int = raw.compare(other.raw).toInt()



  actual override fun toString(): String = raw.stringValue

  override fun toInt(): Int {
    return raw.intValue
  }

  override fun toLong(): Long {
    return raw.longValue
  }

  override fun toShort(): Short {
    return raw.shortValue
  }

  override fun toByte(): Byte {
    return raw.charValue
  }

  override fun toChar(): Char {
    return raw.charValue.toChar()
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
}
