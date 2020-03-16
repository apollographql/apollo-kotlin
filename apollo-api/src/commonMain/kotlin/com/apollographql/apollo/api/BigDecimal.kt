package com.apollographql.apollo.api

expect class BigDecimal {
  constructor(strVal: String)
  constructor(doubleVal: Double)
  constructor(intVal: Int)
  constructor(longVal: Long)

  fun add(augend: BigDecimal): BigDecimal
  fun subtract(subtrahend: BigDecimal): BigDecimal
  fun multiply(multiplicand: BigDecimal): BigDecimal
  fun divide(divisor: BigDecimal): BigDecimal
  fun negate(): BigDecimal
  fun signum(): Int

  override fun toString(): String

  fun toInt(): Int
  fun toLong(): Long
  fun toDouble(): Double
}
