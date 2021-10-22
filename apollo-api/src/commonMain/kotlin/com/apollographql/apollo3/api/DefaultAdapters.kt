@file:JvmName("Adapters")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.MapJsonReader.Companion.buffer
import com.apollographql.apollo3.api.internal.json.MapJsonWriter
import com.apollographql.apollo3.api.internal.json.Utils
import com.apollographql.apollo3.api.internal.json.Utils.readRecursively
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSuppressWildcards
import kotlin.native.concurrent.SharedImmutable

/**
 * This file contains a list of [Adapter] for standard types
 *
 * They are mostly used from the generated code but could be useful in any other situations that requires adapting from
 * GraphQL to Kotlin.
 * In particular, [AnyAdapter] can be used to read/write a Kotlin representation from/to Json.
 */
class ListAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<List<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): List<T> {
    reader.beginArray()
    val list = mutableListOf<T>()
    while (reader.hasNext()) {
      list.add(wrappedAdapter.fromJson(reader, customScalarAdapters))
    }
    reader.endArray()
    return list
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: List<T>) {
    writer.beginArray()
    value.forEach {
      wrappedAdapter.toJson(writer, customScalarAdapters, it)
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

  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T? {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      null
    } else {
      wrappedAdapter.fromJson(reader, customScalarAdapters)
    }
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T?) {
    if (value == null) {
      writer.nullValue()
    } else {
      wrappedAdapter.toJson(writer, customScalarAdapters, value)
    }
  }
}

/**
 * ResponseAdapters can only express something that's present. Absent values are handled outside of the adapter
 */
class OptionalAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<Optional.Present<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Optional.Present<T> {
    return Optional.Present(wrappedAdapter.fromJson(reader, customScalarAdapters))
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Optional.Present<T>) {
    wrappedAdapter.toJson(writer, customScalarAdapters, value.value)
  }
}

object StringAdapter : Adapter<String> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): String {
    return reader.nextString()!!
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: String) {
    writer.value(value)
  }
}

object IntAdapter : Adapter<Int> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Int {
    return reader.nextInt()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Int) {
    writer.value(value)
  }
}

object DoubleAdapter : Adapter<Double> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Double {
    return reader.nextDouble()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Double) {
    writer.value(value)
  }
}

/**
 * An [Adapter] that converts to/from a [Long]
 *
 * If the Json number does not fit in a [Long], an exception will be thrown
 */
object LongAdapter: Adapter<Long>  {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Long {
    return reader.nextLong()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Long) {
    writer.value(value)
  }
}

object BooleanAdapter : Adapter<Boolean> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Boolean {
    return reader.nextBoolean()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Boolean) {
    writer.value(value)
  }
}

object AnyAdapter : Adapter<Any> {
  fun fromJson(reader: JsonReader): Any {
    return reader.readRecursively()!!
  }

  fun toJson(writer: JsonWriter, value: Any) {
    Utils.writeToJson(value, writer)
  }

  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Any {
    return fromJson(reader)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Any) {
    toJson(writer, value)
  }
}

object UploadAdapter : Adapter<Upload> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Upload {
    error("File Upload used in output position")
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Upload) {
    writer.value(value)
  }
}

class ObjectAdapter<T>(
    private val wrappedAdapter: Adapter<T>,
    private val buffered: Boolean,
) : Adapter<@JvmSuppressWildcards T> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T {
    val actualReader = if (buffered) {
      reader.buffer()
    } else {
      reader
    }
    actualReader.beginObject()
    return wrappedAdapter.fromJson(actualReader, customScalarAdapters).also {
      actualReader.endObject()
    }
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T) {
    if (buffered && writer !is MapJsonWriter) {
      /**
       * Convert to a Map first
       */
      val mapWriter = MapJsonWriter()
      mapWriter.beginObject()
      wrappedAdapter.toJson(mapWriter, customScalarAdapters, value)
      mapWriter.endObject()

      /**
       * And write to the original writer
       */
      Utils.writeToJson(mapWriter.root()!!, writer)
    } else {
      writer.beginObject()
      wrappedAdapter.toJson(writer, customScalarAdapters, value)
      writer.endObject()
    }
  }
}

fun <T : Any> Adapter<T>.nullable() = NullableAdapter(this)
fun <T> Adapter<T>.list() = ListAdapter(this)
fun <T> Adapter<T>.obj(buffered: Boolean = false) = ObjectAdapter(this, buffered)
fun <T> Adapter<T>.optional() = OptionalAdapter(this)

/**
 * Global instances of nullable adapters for built-in scalar types
 */
@SharedImmutable
@JvmField
val NullableStringAdapter = StringAdapter.nullable()
@SharedImmutable
@JvmField
val NullableDoubleAdapter = DoubleAdapter.nullable()
@SharedImmutable
@JvmField
val NullableIntAdapter = IntAdapter.nullable()
@SharedImmutable
@JvmField
val NullableBooleanAdapter = BooleanAdapter.nullable()
@SharedImmutable
@JvmField
val NullableAnyAdapter = AnyAdapter.nullable()

