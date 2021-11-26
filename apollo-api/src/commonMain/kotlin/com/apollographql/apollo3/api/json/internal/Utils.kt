package com.apollographql.apollo3.api.json.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.json.JsonNumber
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import kotlin.jvm.JvmName


@JvmName("-LongToIntExact")
internal fun Long.toIntExact(): Int {
  val result = toInt()
  check (result.toLong() == this) {
    "$this cannot be converted to Int"
  }
  return result
}

@JvmName("-DoubleToIntExact")
internal fun Double.toIntExact(): Int {
  val result = toInt()
  check (result.toDouble() == this) {
    "$this cannot be converted to Int"
  }
  return result
}


@JvmName("-LongToDoubleExact")
internal fun Long.toDoubleExact(): Double {
  val result = toDouble()
  check (result.toLong() == this) {
    "$this cannot be converted to Double"
  }
  return result
}

@JvmName("-DoubleToLongExact")
internal fun Double.toLongExact(): Long {
  val result = toLong()
  check (result.toDouble() == this) {
    "$this cannot be converted to Long"
  }
  return result
}