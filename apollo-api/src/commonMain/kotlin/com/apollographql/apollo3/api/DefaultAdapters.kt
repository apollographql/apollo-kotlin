package com.apollographql.apollo3.api


import com.apollographql.apollo3.api.internal.json.MapJsonReader.Companion.buffer
import com.apollographql.apollo3.api.internal.json.MapJsonWriter
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.internal.json.Utils
import com.apollographql.apollo3.api.internal.json.Utils.readRecursively
import kotlin.native.concurrent.SharedImmutable

/**
 * This file contains a list of [Adapter] for standard types
 *
 * They are mostly used from the generated code but could be useful in any other situations that requires adapting from
 * GraphQL to Kotlin.
 * In particular, [AnyAdapter] can be used to read/write a Kotlin representation from/to Json.
 */
class ListAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<List<T>> {
  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): List<T> {
    reader.beginArray()
    val list = mutableListOf<T>()
    while (reader.hasNext()) {
      list.add(wrappedAdapter.fromJson(reader, responseAdapterCache))
    }
    reader.endArray()
    return list
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: List<T>) {
    writer.beginArray()
    value.forEach {
      wrappedAdapter.toJson(writer, responseAdapterCache, it)
    }
    writer.endArray()
  }
}

class NullableAdapter<T : Any>(private val wrappedAdapter: Adapter<T>) : Adapter<T?> {
  init {
    check(wrappedAdapter !is NullableAdapter<*>) {
      "The adapter is already nullable"
    }
  }

  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): T? {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      null
    } else {
      wrappedAdapter.fromJson(reader, responseAdapterCache)
    }
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: T?) {
    if (value == null) {
      writer.nullValue()
    } else {
      wrappedAdapter.toJson(writer, responseAdapterCache, value)
    }
  }
}

/**
 * ResponseAdapters can only express something that's present. Absent values are handled outside of the adapter
 */
class OptionalAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<Optional.Present<T>> {
  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): Optional.Present<T> {
    return Optional.Present(wrappedAdapter.fromJson(reader, responseAdapterCache))
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: Optional.Present<T>) {
    wrappedAdapter.toJson(writer, responseAdapterCache, value.value)
  }
}

object StringAdapter : Adapter<String> {
  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): String {
    return reader.nextString()!!
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: String) {
    writer.value(value)
  }
}

object IntAdapter : Adapter<Int> {
  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): Int {
    return reader.nextInt()
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: Int) {
    writer.value(value)
  }
}

object DoubleAdapter : Adapter<Double> {
  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): Double {
    return reader.nextDouble()
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: Double) {
    writer.value(value)
  }
}

object BooleanAdapter : Adapter<Boolean> {
  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): Boolean {
    return reader.nextBoolean()
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: Boolean) {
    writer.value(value)
  }
}

object AnyAdapter : Adapter<Any> {
  fun fromResponse(reader: JsonReader): Any {
    return reader.readRecursively()!!
  }

  fun toResponse(writer: JsonWriter, value: Any) {
    Utils.writeToJson(value, writer)
  }

  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): Any {
    return fromResponse(reader)
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: Any) {
    toResponse(writer, value)
  }
}

object UploadAdapter : Adapter<Upload> {
  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): Upload {
    error("File Upload used in output position")
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: Upload) {
    writer.value(value)
  }
}

class ObjectAdapter<T>(
    private val wrappedAdapter: Adapter<T>,
    private val buffered: Boolean
) : Adapter<T> {
  override fun fromJson(reader: JsonReader, responseAdapterCache: CustomScalarAdpaters): T {
    val actualReader = if (buffered) {
      reader.buffer()
    } else {
      reader
    }
    actualReader.beginObject()
    return wrappedAdapter.fromJson(actualReader, responseAdapterCache).also {
      actualReader.endObject()
    }
  }

  override fun toJson(writer: JsonWriter, responseAdapterCache: CustomScalarAdpaters, value: T) {
    if (buffered && writer !is MapJsonWriter) {
      /**
       * Convert to a Map first
       */
      val mapWriter = MapJsonWriter()
      mapWriter.beginObject()
      wrappedAdapter.toJson(mapWriter, responseAdapterCache, value)
      mapWriter.endObject()

      /**
       * And write to the original writer
       */
      AnyAdapter.toResponse(writer, mapWriter.root()!!)
    } else {
      writer.beginObject()
      wrappedAdapter.toJson(writer, responseAdapterCache, value)
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
val NullableStringResponseAdapter = StringAdapter.nullable()
@SharedImmutable
val NullableDoubleResponseAdapter = DoubleAdapter.nullable()
@SharedImmutable
val NullableIntResponseAdapter = IntAdapter.nullable()
@SharedImmutable
val NullableBooleanResponseAdapter = BooleanAdapter.nullable()
@SharedImmutable
val NullableAnyResponseAdapter = AnyAdapter.nullable()
