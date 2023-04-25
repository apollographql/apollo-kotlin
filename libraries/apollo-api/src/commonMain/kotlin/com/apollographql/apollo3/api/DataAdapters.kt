@file:JvmName("DataAdapters")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.DataAdapter.DeserializeDataContext
import com.apollographql.apollo3.api.DataAdapter.SerializeDataContext
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.MapJsonReader.Companion.buffer
import com.apollographql.apollo3.api.json.MapJsonWriter
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.writeAny
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmSuppressWildcards

/**
 * This file contains a list of [DataAdapter] for standard types
 *
 * They are mostly used from the generated code but could be useful in any other situations that requires adapting from
 * GraphQL to Kotlin.
 * In particular, [AnyDataAdapter] can be used to read/write a Kotlin representation from/to Json.
 */
class ListDataAdapter<T>(private val wrappedAdapter: DataAdapter<T>) : DataAdapter<List<@JvmSuppressWildcards T>> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): List<T> {
    reader.beginArray()
    val list = mutableListOf<T>()
    while (reader.hasNext()) {
      list.add(wrappedAdapter.deserializeData(reader, context))
    }
    reader.endArray()
    return list
  }

  override fun serializeData(writer: JsonWriter, value: List<T>, context: SerializeDataContext) {
    writer.beginArray()
    value.forEach {
      wrappedAdapter.serializeData(writer, it, context)
    }
    writer.endArray()
  }
}

class NullableDataAdapter<T : Any>(private val wrappedAdapter: DataAdapter<T>) : DataAdapter<@JvmSuppressWildcards T?> {
  init {
    check(wrappedAdapter !is NullableDataAdapter<*>) {
      "The adapter is already nullable"
    }
  }

  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): T? {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      null
    } else {
      wrappedAdapter.deserializeData(reader, context)
    }
  }

  override fun serializeData(writer: JsonWriter, value: T?, context: SerializeDataContext) {
    if (value == null) {
      writer.nullValue()
    } else {
      wrappedAdapter.serializeData(writer, value, context)
    }
  }
}

@Deprecated("Use PresentAdapter instead")
class OptionalDataAdapter<T>(private val wrappedAdapter: DataAdapter<T>) : DataAdapter<Optional.Present<@JvmSuppressWildcards T>> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): Optional.Present<T> {
    return Optional.Present(wrappedAdapter.deserializeData(reader, context))
  }

  override fun serializeData(writer: JsonWriter, value: Optional.Present<T>, context: SerializeDataContext) {
    wrappedAdapter.serializeData(writer, value.value, context)
  }
}

/**
 * PresentAdapter can only express something that's present. Absent values are handled outside of the adapter.
 *
 * This adapter is used to handle optional arguments in operations and optional fields in Input objects.
 */
class PresentDataAdapter<T>(private val wrappedAdapter: DataAdapter<T>) : DataAdapter<Optional.Present<@JvmSuppressWildcards T>> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): Optional.Present<T> {
    return Optional.Present(wrappedAdapter.deserializeData(reader, context))
  }

  override fun serializeData(writer: JsonWriter, value: Optional.Present<T>, context: SerializeDataContext) {
    wrappedAdapter.serializeData(writer, value.value, context)
  }
}


/**
 * This adapter is used to handle nullable fields when they are represented as [Optional].
 * `null` is deserialized as [Optional.Absent].
 */
class ApolloOptionalDataAdapter<T>(private val wrappedAdapter: DataAdapter<T>) : DataAdapter<Optional<@JvmSuppressWildcards T>> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): Optional<T> {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      Optional.Absent
    } else {
      Optional.Present(wrappedAdapter.deserializeData(reader, context))
    }
  }

  override fun serializeData(writer: JsonWriter, value: Optional<T>, context: SerializeDataContext) {
    if (value is Optional.Present) {
      wrappedAdapter.serializeData(writer, value.value, context)
    } else {
      writer.nullValue()
    }
  }
}

class AdapterToDataAdapter<T>(private val wrappedAdapter: Adapter<T>) : DataAdapter<T> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): T {
    return wrappedAdapter.fromJson(reader, CustomScalarAdapters.Empty)
  }

  override fun serializeData(writer: JsonWriter, value: T, context: SerializeDataContext) {
    wrappedAdapter.toJson(writer, CustomScalarAdapters.Empty, value)
  }
}

class ObjectDataAdapter<T>(
    private val wrappedAdapter: DataAdapter<T>,
    private val buffered: Boolean,
) : DataAdapter<@JvmSuppressWildcards T> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): T {
    val actualReader = if (buffered) {
      reader.buffer()
    } else {
      reader
    }
    actualReader.beginObject()
    return wrappedAdapter.deserializeData(actualReader, context).also {
      actualReader.endObject()
    }
  }

  override fun serializeData(writer: JsonWriter, value: T, context: SerializeDataContext) {
    if (buffered && writer !is MapJsonWriter) {
      /**
       * Convert to a Map first
       */
      val mapWriter = MapJsonWriter()
      mapWriter.beginObject()
      wrappedAdapter.serializeData(mapWriter, value, context)
      mapWriter.endObject()

      /**
       * And write to the original writer
       */
      writer.writeAny(mapWriter.root()!!)
    } else {
      writer.beginObject()
      wrappedAdapter.serializeData(writer, value, context)
      writer.endObject()
    }
  }
}

@JvmName("-nullable")
fun <T : Any> DataAdapter<T>.nullable() = NullableDataAdapter(this)

@JvmName("-list")
fun <T> DataAdapter<T>.list() = ListDataAdapter(this)

@JvmName("-obj")
fun <T> DataAdapter<T>.obj(buffered: Boolean = false) = ObjectDataAdapter(this, buffered)

@JvmName("-optional")
@Deprecated("Use present instead", ReplaceWith("present()"))
fun <T> DataAdapter<T>.optional() = PresentDataAdapter(this)

@JvmName("-present")
fun <T> DataAdapter<T>.present() = PresentDataAdapter(this)


@JvmName("-toJson")
@JvmOverloads
fun <T> DataAdapter<T>.toJsonString(
    value: T,
    context: SerializeDataContext = SerializeDataContext(customScalarAdapters = CustomScalarAdapters.Empty),
    indent: String? = null,
): String = buildJsonString(indent) {
  this@toJsonString.serializeData(this, value, context)
}
