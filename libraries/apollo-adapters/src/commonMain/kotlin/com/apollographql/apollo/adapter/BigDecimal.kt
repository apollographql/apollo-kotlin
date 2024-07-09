@file:Suppress("DEPRECATION")

package com.apollographql.apollo.adapter

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter


/**
 * An [Adapter] that converts to/from [BigDecimal]
 */
@Deprecated("BigDecimalAdapter has new maven coordinates at 'com.apollographql.adapters:apollo-adapters-core. See https://go.apollo.dev/ak-moved-artifacts for more details.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
object BigDecimalAdapter : Adapter<BigDecimal> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): BigDecimal {
    return BigDecimal(reader.nextString()!!)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: BigDecimal) {
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
@Deprecated("BigDecimal has new maven coordinates at 'com.apollographql.adapters:apollo-adapters-core. See https://go.apollo.dev/ak-moved-artifacts for more details.")
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
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
