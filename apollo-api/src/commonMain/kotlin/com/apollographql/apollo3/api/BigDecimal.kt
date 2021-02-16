package com.apollographql.apollo3.api

// BigDecimal cannot subclass `Number` in JS, as it will cause runtime trap in any compiled Kotlin/JS product in the module initialization
// script.

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
}

expect fun BigDecimal.toNumber(): Number
