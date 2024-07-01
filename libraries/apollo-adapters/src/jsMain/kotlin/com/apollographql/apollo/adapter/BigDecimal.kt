package com.apollographql.apollo.adapter

import com.apollographql.apollo.annotations.ApolloDeprecatedSince

@JsModule("big.js")
@JsNonModule
internal external fun jsBig(raw: dynamic): Big

@JsName("Number")
internal external fun jsNumber(raw: dynamic): Number

internal external class Big {
  fun plus(other: Big): Big
  fun minus(other: Big): Big
  fun times(other: Big): Big
  fun div(other: Big): Big
  fun cmp(other: Big): Int
  fun eq(other: Big): Boolean
  fun round(dp: Int, rm: Int): Big
}

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
actual class BigDecimal {
  internal val raw: Big

  internal constructor(raw: Big) {
    this.raw = raw
  }

  constructor() : this(raw = jsBig(0))

  actual constructor(strVal: String) : this(raw = jsBig(strVal))

  actual constructor(doubleVal: Double) {
    check(!doubleVal.isNaN() && !doubleVal.isInfinite())
    raw = jsBig(doubleVal)
  }

  actual constructor(intVal: Int) : this(raw = jsBig(intVal))

  // JS does not support 64-bit integer natively.
  actual constructor(longVal: Long) : this(raw = jsBig(longVal.toString()))

  actual fun add(augend: BigDecimal): BigDecimal = BigDecimal(raw = raw.plus(augend.raw))

  actual fun subtract(subtrahend: BigDecimal): BigDecimal = BigDecimal(raw = raw.minus(subtrahend.raw))

  actual fun multiply(multiplicand: BigDecimal): BigDecimal = BigDecimal(raw = raw.times(multiplicand.raw))

  actual fun divide(divisor: BigDecimal): BigDecimal = BigDecimal(raw = raw.div(divisor.raw))

  actual fun negate(): BigDecimal = BigDecimal().subtract(this)

  actual fun signum(): Int {
    return raw.cmp(jsBig(0))
  }

  fun toInt(): Int {
    return jsNumber(raw).toInt()
  }

  fun toLong(): Long {
    // JSNumber is double precision, so it cannot exactly represent 64-bit `Long`.
    return toString().toLong()
  }

  fun toShort(): Short {
    return jsNumber(raw).toShort()
  }

  fun toByte(): Byte {
    return jsNumber(raw).toByte()
  }

  fun toChar(): Char {
    return jsNumber(raw).toInt().toChar()
  }

  fun toDouble(): Double {
    return jsNumber(raw).toDouble()
  }

  fun toFloat(): Float {
    return jsNumber(raw).toFloat()
  }

  override fun equals(other: Any?): Boolean {
    if (other is BigDecimal) {
      return raw.eq(other.raw)
    }
    return false
  }

  override fun hashCode(): Int = raw.toString().hashCode()

  override fun toString(): String = raw.toString()
}

actual fun BigDecimal.toNumber(): Number {
  val rounded = raw.round(0, 0)

  return if (raw.minus(rounded).eq(jsBig(0))) {
    toLong()
  } else {
    toDouble()
  }
}
