package com.apollographql.apollo.api

import com.soywiz.kbignum.BigInt
import com.soywiz.kbignum.BigNum

@ExperimentalUnsignedTypes
actual class BigDecimal(private val raw: BigNum) : Number(), Comparable<BigDecimal> {

  actual constructor(strVal: String) : this(BigNum(strVal))

  actual constructor(doubleVal: Double) : this(BigNum(doubleVal.toString().let { str -> if ("." in str) str else "$str.0" }))

  actual constructor(intVal: Int) : this(BigNum(BigInt(intVal), 0))

  actual constructor(longVal: Long) : this(BigNum(BigInt(longVal), 0))

  actual fun add(augend: BigDecimal): BigDecimal = BigDecimal(this.raw + augend.raw)

  actual fun subtract(subtrahend: BigDecimal): BigDecimal = BigDecimal(this.raw - subtrahend.raw)

  actual fun multiply(multiplicand: BigDecimal): BigDecimal = BigDecimal(this.raw * multiplicand.raw)

  actual fun divide(divisor: BigDecimal): BigDecimal = BigDecimal(this.raw / divisor.raw)

  actual fun negate(): BigDecimal = BigDecimal(this.raw * rawMinusOne)

  actual fun signum(): Int =
    when {
      this.raw.int.isNegative -> -1
      this.raw.int.isPositive -> 1
      else -> 0
    }

  override fun equals(other: Any?): Boolean = this.raw == (other as? BigDecimal)?.raw

  override fun hashCode(): Int = this.raw.hashCode()

  override fun compareTo(other: BigDecimal): Int = this.raw.compareTo(other.raw)

  override fun toString(): String = this.raw.toString()

  override fun toChar(): Char = toInt().toChar()

  override fun toByte(): Byte = toInt().toByte()

  override fun toShort(): Short = toInt().toShort()

  override fun toInt(): Int = toDouble().toInt()

  override fun toLong(): Long = toDouble().toLong()

  override fun toFloat(): Float = toDouble().toFloat()

  override fun toDouble(): Double = this.raw.toString().toDouble()

  companion object {
    private val rawMinusOne = BigNum("-1")
  }
}

