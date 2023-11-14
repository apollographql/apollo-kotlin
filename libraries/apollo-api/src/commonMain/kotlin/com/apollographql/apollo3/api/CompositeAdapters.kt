package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.MapJsonReader.Companion.buffer
import com.apollographql.apollo3.api.json.MapJsonWriter
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.writeAny
import com.apollographql.apollo3.exception.DefaultApolloException
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmSuppressWildcards

class ListCompositeAdapter<T>(private val wrappedAdapter: CompositeAdapter<T>) : CompositeAdapter<List<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, adapterContext: CompositeAdapterContext): List<T> {
    reader.beginArray()
    val list = mutableListOf<T>()
    while (reader.hasNext()) {
      list.add(wrappedAdapter.fromJson(reader, adapterContext))
    }
    reader.endArray()
    return list
  }

  override fun toJson(writer: JsonWriter, value: List<T>, adapterContext: CompositeAdapterContext) {
    writer.beginArray()
    value.forEach {
      wrappedAdapter.toJson(writer, it, adapterContext)
    }
    writer.endArray()
  }
}

class NullableCompositeAdapter<T : Any>(private val wrappedAdapter: CompositeAdapter<T>) : CompositeAdapter<@JvmSuppressWildcards T?> {
  init {
    check(wrappedAdapter !is NullableCompositeAdapter<*>) {
      "The adapter is already nullable"
    }
  }

  override fun fromJson(reader: JsonReader, adapterContext: CompositeAdapterContext): T? {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      null
    } else {
      wrappedAdapter.fromJson(reader, adapterContext)
    }
  }

  override fun toJson(writer: JsonWriter, value: T?, adapterContext: CompositeAdapterContext) {
    if (value == null) {
      writer.nullValue()
    } else {
      wrappedAdapter.toJson(writer, value, adapterContext)
    }
  }
}

class ResultCompositeAdapter<T : Any>(private val wrappedAdapter: CompositeAdapter<T>): CompositeAdapter<Result<T>> {
  override fun fromJson(reader: JsonReader, adapterContext: CompositeAdapterContext): Result<T> {
    try {
      return Result.Data(wrappedAdapter.fromJson(reader, adapterContext))
    } catch (e: Exception) {
      val errors = adapterContext.errorsForPath(reader.getPath())
      if (errors.isNotEmpty()) {
        reader.skipValue()
        return Result.Error(errors)
      }

      throw DefaultApolloException("no error found for @catch field at '${reader.getPath()}'")
    }
  }

  override fun toJson(writer: JsonWriter, value: Result<T>, adapterContext: CompositeAdapterContext) {
    // We do not support serializing errors
    wrappedAdapter.toJson(writer, value.getOrThrow(), adapterContext)
  }
}

class PresentCompositeAdapter<T>(private val wrappedAdapter: CompositeAdapter<T>) : CompositeAdapter<Optional.Present<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, adapterContext: CompositeAdapterContext): Optional.Present<T> {
    return Optional.Present(wrappedAdapter.fromJson(reader, adapterContext))
  }

  override fun toJson(writer: JsonWriter, value: Optional.Present<T>, adapterContext: CompositeAdapterContext) {
    wrappedAdapter.toJson(writer, value.value, adapterContext)
  }
}

class ApolloOptionalCompositeAdapter<T>(private val wrappedAdapter: CompositeAdapter<T>) : CompositeAdapter<Optional<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, adapterContext: CompositeAdapterContext): Optional<T> {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      Optional.Absent
    } else {
      Optional.Present(wrappedAdapter.fromJson(reader, adapterContext))
    }
  }

  override fun toJson(writer: JsonWriter, value: Optional<T>, adapterContext: CompositeAdapterContext) {
    if (value is Optional.Present) {
      wrappedAdapter.toJson(writer, value.value, adapterContext)
    } else {
      writer.nullValue()
    }
  }
}

class AdapterToCompositeAdapter<T>(private val wrappedAdapter: Adapter<T>) : CompositeAdapter<T> {
  override fun fromJson(reader: JsonReader, adapterContext: CompositeAdapterContext): T {
    return wrappedAdapter.fromJson(reader, CustomScalarAdapters.Empty)
  }

  override fun toJson(writer: JsonWriter, value: T, adapterContext: CompositeAdapterContext) {
    wrappedAdapter.toJson(writer, CustomScalarAdapters.Empty, value)
  }
}

class ObjectCompositeAdapter<T>(
    private val wrappedAdapter: CompositeAdapter<T>,
    private val buffered: Boolean,
) : CompositeAdapter<@JvmSuppressWildcards T> {
  override fun fromJson(reader: JsonReader, adapterContext: CompositeAdapterContext): T {
    val actualReader = if (buffered) {
      reader.buffer()
    } else {
      reader
    }
    actualReader.beginObject()
    return wrappedAdapter.fromJson(actualReader, adapterContext).also {
      actualReader.endObject()
    }
  }

  override fun toJson(writer: JsonWriter, value: T, adapterContext: CompositeAdapterContext) {
    if (buffered && writer !is MapJsonWriter) {
      /**
       * Convert to a Map first
       */
      val mapWriter = MapJsonWriter()
      mapWriter.beginObject()
      wrappedAdapter.toJson(mapWriter, value, adapterContext)
      mapWriter.endObject()

      /**
       * And write to the original writer
       */
      writer.writeAny(mapWriter.root()!!)
    } else {
      writer.beginObject()
      wrappedAdapter.toJson(writer, value, adapterContext)
      writer.endObject()
    }
  }
}

@JvmName("-result")
fun <T : Any> CompositeAdapter<T>.result() = ResultCompositeAdapter(this)

@JvmName("-nullable")
fun <T : Any> CompositeAdapter<T>.nullable() = NullableCompositeAdapter(this)

@JvmName("-list")
fun <T> CompositeAdapter<T>.list() = ListCompositeAdapter(this)

@JvmName("-obj")
fun <T> CompositeAdapter<T>.obj(buffered: Boolean = false) = ObjectCompositeAdapter(this, buffered)

@JvmName("-present")
fun <T> CompositeAdapter<T>.present() = PresentCompositeAdapter(this)



@JvmName("-toJson")
@JvmOverloads
fun <T> CompositeAdapter<T>.toJsonString(
    value: T,
    adapterContext: CompositeAdapterContext = CompositeAdapterContext.Builder().build(),
    indent: String? = null,
): String = buildJsonString(indent) {
  this@toJsonString.toJson(this, value, adapterContext)
}

/**
 * Note that Arrays require their type to be known at compile time, so we construct an anonymous object with reference to
 * function with reified type parameters as a workaround.
 *
 */
@JvmName("-array2")
inline fun <reified T> CompositeAdapter<T>.array() = object : CompositeAdapter<Array<T>> {

  private inline fun <reified T> arrayFromJson(wrappedAdapter: CompositeAdapter<T>, reader: JsonReader, adapterContext: CompositeAdapterContext): Array<T> {
    reader.beginArray()
    val list = mutableListOf<T>()
    while (reader.hasNext()) {
      list.add(wrappedAdapter.fromJson(reader, adapterContext))
    }
    reader.endArray()
    return list.toTypedArray()
  }

  private inline fun <reified T> arrayToJson(
      wrappedAdapter: CompositeAdapter<T>,
      writer: JsonWriter,
      value: Array<T>,
      adapterContext: CompositeAdapterContext,
  ) {
    writer.beginArray()
    value.forEach {
      wrappedAdapter.toJson(writer, it, adapterContext)
    }
    writer.endArray()
  }

  override fun fromJson(reader: JsonReader, adapterContext: CompositeAdapterContext): Array<T> {
    return arrayFromJson(this@array, reader, adapterContext)
  }

  override fun toJson(writer: JsonWriter, value: Array<T>, adapterContext: CompositeAdapterContext) {
    return arrayToJson(this@array, writer, value, adapterContext)
  }
}
