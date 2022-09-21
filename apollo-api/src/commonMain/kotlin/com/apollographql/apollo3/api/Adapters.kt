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

@JvmField
val StringAdapter = object  : Adapter<String> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): String {
    return reader.nextString()!!
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: String) {
    writer.value(value)
  }
}

@JvmField
val IntAdapter = object  : Adapter<Int> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Int {
    return reader.nextInt()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Int) {
    writer.value(value)
  }
}

@JvmField
val DoubleAdapter = object  : Adapter<Double> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Double {
    return reader.nextDouble()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Double) {
    writer.value(value)
  }
}


@JvmField
val FloatAdapter = object  : Adapter<Float> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Float {
    return reader.nextDouble().toFloat()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Float) {
    writer.value(value.toDouble())
  }
}


@JvmField
val LongAdapter = object : Adapter<Long>  {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Long {
    return reader.nextLong()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Long) {
    writer.value(value)
  }
}

@JvmField
val BooleanAdapter = object  : Adapter<Boolean> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Boolean {
    return reader.nextBoolean()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Boolean) {
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

  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Any {
    return fromJson(reader)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Any) {
    toJson(writer, value)
  }
}

internal class PassThroughAdapter<T>: Adapter<T> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T {
    check (reader is MapJsonReader) {
      "UnsafeAdapter only supports MapJsonReader"
    }

    @Suppress("UNCHECKED_CAST")
    return reader.nextValue() as T
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T) {
    check (writer is MapJsonWriter) {
      "UnsafeAdapter only supports MapJsonWriter"
    }

    writer.value(value)
  }
}

@JvmField
val UploadAdapter = object  : Adapter<Upload> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Upload {
    error("File Upload used in output position")
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Upload) {
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
      writer.writeAny(mapWriter.root()!!)
    } else {
      writer.beginObject()
      wrappedAdapter.toJson(writer, customScalarAdapters, value)
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
fun <T> Adapter<T>.optional() = OptionalAdapter(this)


@JvmName("-toJson")
@JvmOverloads
fun <T> Adapter<T>.toJsonString(value: T, customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty, indent: String? = null): String = buildJsonString(indent) {
  this@toJsonString.toJson(this, customScalarAdapters, value)
}
