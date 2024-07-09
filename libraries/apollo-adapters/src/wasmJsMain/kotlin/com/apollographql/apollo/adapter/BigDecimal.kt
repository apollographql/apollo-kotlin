package com.apollographql.apollo.adapter

import com.apollographql.apollo.annotations.ApolloDeprecatedSince

@JsModule("big.js")
internal external fun Big(raw: JsAny): Big

@JsName("Number")
internal external fun jsNumber(raw: Big): JsNumber

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

  constructor() : this(raw = Big())

  actual constructor(strVal: String) : this(raw = Big(strVal.toJsString()))

  actual constructor(doubleVal: Double) {
    check(!doubleVal.isNaN() && !doubleVal.isInfinite())
    raw = Big(doubleVal.toJsNumber())
  }

  actual constructor(intVal: Int) : this(raw = Big(intVal.toJsNumber()))

  // JS does not support 64-bit integer natively.
  actual constructor(longVal: Long) : this(raw = Big(longVal.toString().toJsString()))

  actual fun add(augend: BigDecimal): BigDecimal = BigDecimal(raw = raw.plus(augend.raw))

  actual fun subtract(subtrahend: BigDecimal): BigDecimal = BigDecimal(raw = raw.minus(subtrahend.raw))

  actual fun multiply(multiplicand: BigDecimal): BigDecimal = BigDecimal(raw = raw.times(multiplicand.raw))

  actual fun divide(divisor: BigDecimal): BigDecimal = BigDecimal(raw = raw.div(divisor.raw))

  actual fun negate(): BigDecimal = BigDecimal().subtract(this)

  actual fun signum(): Int {
    return raw.cmp(Big())
  }

  fun toInt(): Int {
    return jsNumber(raw).toInt()
  }

  fun toLong(): Long {
    // JSNumber is double precision, so it cannot exactly represent 64-bit `Long`.
    return toString().toLong()
  }

  fun toShort(): Short {
    return jsNumber(raw).toInt().toShort()
  }

  fun toByte(): Byte {
    return jsNumber(raw).toInt().toByte()
  }

  fun toChar(): Char {
    return jsNumber(raw).toInt().toChar()
  }

  fun toDouble(): Double {
    return jsNumber(raw).toDouble()
  }

  fun toFloat(): Float {
    return jsNumber(raw).toDouble().toFloat()
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

  return if (raw.minus(rounded).eq(Big())) {
    toLong()
  } else {
    toDouble()
  }
}
