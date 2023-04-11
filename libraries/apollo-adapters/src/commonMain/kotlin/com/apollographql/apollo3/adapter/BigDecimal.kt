package com.apollographql.apollo3.adapter

import com.apollographql.apollo3.api.ScalarAdapter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter


/**
 * A [ScalarAdapter] that converts to/from [BigDecimal]
 */
object BigDecimalAdapter : ScalarAdapter<BigDecimal> {
  override fun fromJson(reader: JsonReader): BigDecimal {
    return BigDecimal(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, value: BigDecimal) {
    writer.value(value.toString())
  }
}

/**
 * A multiplatform BigDecimal
 *
 * It's here for historical reasons mainly as GraphQL doesn't has Big Decimal types and should be moved to a separate module
 *
 * BigDecimal cannot subclass `Number` in JS, as it will cause runtime trap in any compiled Kotlin/JS product in the module initialization
 * script.
 */
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
