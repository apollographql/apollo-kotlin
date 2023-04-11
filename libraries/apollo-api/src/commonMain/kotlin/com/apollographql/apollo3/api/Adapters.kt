@file:JvmName("Adapters")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.json.MapJsonReader.Companion.buffer
import com.apollographql.apollo3.api.json.MapJsonWriter
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.readAny
import com.apollographql.apollo3.api.json.writeAny
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmSuppressWildcards

/**
 * This file contains a list of [Adapter] for standard types
 *
 * They are mostly used from the generated code but could be useful in any other situations that requires adapting from
 * GraphQL to Kotlin.
 * In particular, [AnyAdapter] can be used to read/write a Kotlin representation from/to Json.
 */
class ListAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<List<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): List<T> {
    reader.beginArray()
    val list = mutableListOf<T>()
    while (reader.hasNext()) {
      list.add(wrappedAdapter.fromJson(reader, scalarAdapters))
    }
    reader.endArray()
    return list
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: List<T>) {
    writer.beginArray()
    value.forEach {
      wrappedAdapter.toJson(writer, scalarAdapters, it)
    }
    writer.endArray()
  }
}

class NullableAdapter<T : Any>(private val wrappedAdapter: Adapter<T>) : Adapter<@JvmSuppressWildcards T?> {
  init {
    check(wrappedAdapter !is NullableAdapter<*>) {
      "The adapter is already nullable"
    }
  }

  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): T? {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      null
    } else {
      wrappedAdapter.fromJson(reader, scalarAdapters)
    }
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: T?) {
    if (value == null) {
      writer.nullValue()
    } else {
      wrappedAdapter.toJson(writer, scalarAdapters, value)
    }
  }
}

@Deprecated("Use PresentAdapter instead")
class OptionalAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<Optional.Present<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): Optional.Present<T> {
    return Optional.Present(wrappedAdapter.fromJson(reader, scalarAdapters))
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: Optional.Present<T>) {
    wrappedAdapter.toJson(writer, scalarAdapters, value.value)
  }
}

/**
 * PresentAdapter can only express something that's present. Absent values are handled outside of the adapter.
 *
 * This adapter is used to handle optional arguments in operations and optional fields in Input objects.
 */
class PresentAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<Optional.Present<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): Optional.Present<T> {
    return Optional.Present(wrappedAdapter.fromJson(reader, scalarAdapters))
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: Optional.Present<T>) {
    wrappedAdapter.toJson(writer, scalarAdapters, value.value)
  }
}


/**
 * This adapter is used to handle nullable fields when they are represented as [Optional].
 * `null` is deserialized as [Optional.Absent].
 */
class ApolloOptionalAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<Optional<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): Optional<T> {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      Optional.Absent
    } else {
      Optional.Present(wrappedAdapter.fromJson(reader, scalarAdapters))
    }
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: Optional<T>) {
    if (value is Optional.Present) {
      wrappedAdapter.toJson(writer, scalarAdapters, value.value)
    } else {
      writer.nullValue()
    }
  }
}

@JvmField
val StringAdapter = object : Adapter<String> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): String {
    return reader.nextString()!!
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: String) {
    writer.value(value)
  }
}

@JvmField
val IntAdapter = object : Adapter<Int> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): Int {
    return reader.nextInt()
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: Int) {
    writer.value(value)
  }
}

@JvmField
val DoubleAdapter = object : Adapter<Double> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): Double {
    return reader.nextDouble()
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: Double) {
    writer.value(value)
  }
}

/**
 * An [Adapter] that converts to/from a [Float]
 * Floats are not part of the GraphQL spec but this can be used in custom scalars
 */
@JvmField
val FloatAdapter = object : Adapter<Float> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): Float {
    return reader.nextDouble().toFloat()
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: Float) {
    writer.value(value.toDouble())
  }
}

/**
 * An [Adapter] that converts to/from a [Long]
 * Longs are not part of the GraphQL spec but this can be used in custom scalars
 *
 * If the Json number does not fit in a [Long], an exception will be thrown
 */
@JvmField
val LongAdapter = object : Adapter<Long> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): Long {
    return reader.nextLong()
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: Long) {
    writer.value(value)
  }
}

@JvmField
val BooleanAdapter = object : Adapter<Boolean> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): Boolean {
    return reader.nextBoolean()
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: Boolean) {
    writer.value(value)
  }
}

@JvmField
val AnyAdapter = object : Adapter<Any> {
  fun fromJson(reader: JsonReader): Any {
    return reader.readAny()!!
  }

  fun toJson(writer: JsonWriter, value: Any) {
    writer.writeAny(value)
  }

  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): Any {
    return fromJson(reader)
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: Any) {
    toJson(writer, value)
  }
}

internal class PassThroughAdapter<T> : Adapter<T> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): T {
    check(reader is MapJsonReader) {
      "UnsafeAdapter only supports MapJsonReader"
    }

    @Suppress("UNCHECKED_CAST")
    return reader.nextValue() as T
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: T) {
    check(writer is MapJsonWriter) {
      "UnsafeAdapter only supports MapJsonWriter"
    }

    writer.value(value)
  }
}

class ScalarAdapterToApolloAdapter<T>(private val wrappedScalarAdapter: ScalarAdapter<T>) : Adapter<T> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): T {
    return wrappedScalarAdapter.fromJson(reader)
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: T) {
    wrappedScalarAdapter.toJson(writer, value)
  }
}

@JvmField
val UploadAdapter = object : Adapter<Upload> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): Upload {
    error("File Upload used in output position")
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: Upload) {
    writer.value(value)
  }
}

/*
 * Global instances of nullable adapters for built-in scalar types
 */
@JvmField
val NullableStringAdapter = StringAdapter.nullable()

@JvmField
val NullableDoubleAdapter = DoubleAdapter.nullable()

@JvmField
val NullableIntAdapter = IntAdapter.nullable()

@JvmField
val NullableBooleanAdapter = BooleanAdapter.nullable()

@JvmField
val NullableAnyAdapter = AnyAdapter.nullable()

/*
 * Global instances of optional adapters for built-in scalar types
 */
@JvmField
val ApolloOptionalStringAdapter = ApolloOptionalAdapter(StringAdapter)

@JvmField
val ApolloOptionalDoubleAdapter = ApolloOptionalAdapter(DoubleAdapter)

@JvmField
val ApolloOptionalIntAdapter = ApolloOptionalAdapter(IntAdapter)

@JvmField
val ApolloOptionalBooleanAdapter = ApolloOptionalAdapter(BooleanAdapter)

@JvmField
val ApolloOptionalAnyAdapter = ApolloOptionalAdapter(AnyAdapter)


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
 * An [Adapter] that converts to/from a [Float]
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
 * An [Adapter] that converts to/from a [Long]
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

class ObjectAdapter<T>(
    private val wrappedAdapter: Adapter<T>,
    private val buffered: Boolean,
) : Adapter<@JvmSuppressWildcards T> {
  override fun fromJson(reader: JsonReader, scalarAdapters: ScalarAdapters): T {
    val actualReader = if (buffered) {
      reader.buffer()
    } else {
      reader
    }
    actualReader.beginObject()
    return wrappedAdapter.fromJson(actualReader, scalarAdapters).also {
      actualReader.endObject()
    }
  }

  override fun toJson(writer: JsonWriter, scalarAdapters: ScalarAdapters, value: T) {
    if (buffered && writer !is MapJsonWriter) {
      /**
       * Convert to a Map first
       */
      val mapWriter = MapJsonWriter()
      mapWriter.beginObject()
      wrappedAdapter.toJson(mapWriter, scalarAdapters, value)
      mapWriter.endObject()

      /**
       * And write to the original writer
       */
      writer.writeAny(mapWriter.root()!!)
    } else {
      writer.beginObject()
      wrappedAdapter.toJson(writer, scalarAdapters, value)
      writer.endObject()
    }
  }
}

@JvmName("-nullable")
fun <T : Any> Adapter<T>.nullable() = NullableAdapter(this)

@JvmName("-list")
fun <T> Adapter<T>.list() = ListAdapter(this)

@JvmName("-obj")
fun <T> Adapter<T>.obj(buffered: Boolean = false) = ObjectAdapter(this, buffered)

@JvmName("-optional")
@Deprecated("Use present instead", ReplaceWith("present()"))
fun <T> Adapter<T>.optional() = PresentAdapter(this)

@JvmName("-present")
fun <T> Adapter<T>.present() = PresentAdapter(this)


@JvmName("-toJson")
@JvmOverloads
fun <T> Adapter<T>.toJsonString(
    value: T,
    scalarAdapters: ScalarAdapters = ScalarAdapters.Empty,
    indent: String? = null,
): String = buildJsonString(indent) {
  this@toJsonString.toJson(this, scalarAdapters, value)
}

@JvmName("-toJson")
@JvmOverloads
fun <T> ScalarAdapter<T>.toJsonString(
    value: T,
    indent: String? = null,
): String = buildJsonString(indent) {
  this@toJsonString.toJson(this, value)
}
