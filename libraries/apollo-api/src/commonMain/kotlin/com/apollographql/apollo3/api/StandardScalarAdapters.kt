@file:JvmName("StandardScalarAdapters")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.readAny
import com.apollographql.apollo3.api.json.writeAny
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/**
 * This file contains a list of [ScalarAdapter] for standard types
 *
 * They are mostly used from the generated code but could be useful in any other situations that requires adapting from
 * GraphQL to Kotlin.
 * In particular, [AnyScalarAdapter] can be used to read/write a Kotlin representation from/to Json.
 */

@JvmField
val StringScalarAdapter = object : ScalarAdapter<String> {
  override fun fromJson(reader: JsonReader): String {
    return reader.nextString()!!
  }

  override fun toJson(writer: JsonWriter, value: String) {
    writer.value(value)
  }
}

@JvmField
val IntScalarAdapter = object : ScalarAdapter<Int> {
  override fun fromJson(reader: JsonReader): Int {
    return reader.nextInt()
  }

  override fun toJson(writer: JsonWriter, value: Int) {
    writer.value(value)
  }
}

@JvmField
val DoubleScalarAdapter = object : ScalarAdapter<Double> {
  override fun fromJson(reader: JsonReader): Double {
    return reader.nextDouble()
  }

  override fun toJson(writer: JsonWriter, value: Double) {
    writer.value(value)
  }
}

/**
 * A [ScalarAdapter] that converts to/from a [Float]
 * Floats are not part of the GraphQL spec but this can be used in custom scalars
 */
@JvmField
val FloatScalarAdapter = object : ScalarAdapter<Float> {
  override fun fromJson(reader: JsonReader): Float {
    return reader.nextDouble().toFloat()
  }

  override fun toJson(writer: JsonWriter, value: Float) {
    writer.value(value.toDouble())
  }
}

/**
 * A [ScalarAdapter] that converts to/from a [Long]
 * Longs are not part of the GraphQL spec but this can be used in custom scalars
 *
 * If the Json number does not fit in a [Long], an exception will be thrown
 */
@JvmField
val LongScalarAdapter = object : ScalarAdapter<Long> {
  override fun fromJson(reader: JsonReader): Long {
    return reader.nextLong()
  }

  override fun toJson(writer: JsonWriter, value: Long) {
    writer.value(value)
  }
}

@JvmField
val BooleanScalarAdapter = object : ScalarAdapter<Boolean> {
  override fun fromJson(reader: JsonReader): Boolean {
    return reader.nextBoolean()
  }

  override fun toJson(writer: JsonWriter, value: Boolean) {
    writer.value(value)
  }
}

@JvmField
val AnyScalarAdapter = object : ScalarAdapter<Any> {
  override fun fromJson(reader: JsonReader): Any {
    return reader.readAny()!!
  }

  override fun toJson(writer: JsonWriter, value: Any) {
    writer.writeAny(value)
  }
}

@JvmField
val UploadScalarAdapter = object : ScalarAdapter<Upload> {
  override fun fromJson(reader: JsonReader): Upload {
    error("File Upload used in output position")
  }

  override fun toJson(writer: JsonWriter, value: Upload) {
    writer.value(value)
  }
}


@JvmName("-toJson")
@JvmOverloads
fun <T> ScalarAdapter<T>.toJsonString(
    value: T,
    indent: String? = null,
): String = buildJsonString(indent) {
  this@toJsonString.toJson(this, value)
}
