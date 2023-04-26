package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.CompositeAdapter.DeserializeCompositeContext
import com.apollographql.apollo3.api.CompositeAdapter.SerializeCompositeContext
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.api.json.MapJsonReader.Companion.buffer
import com.apollographql.apollo3.api.json.MapJsonWriter
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.writeAny
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmSuppressWildcards

class ListCompositeAdapter<T>(private val wrappedAdapter: CompositeAdapter<T>) : CompositeAdapter<List<@JvmSuppressWildcards T>> {
  override fun deserializeComposite(reader: JsonReader, context: DeserializeCompositeContext): List<T> {
    reader.beginArray()
    val list = mutableListOf<T>()
    while (reader.hasNext()) {
      list.add(wrappedAdapter.deserializeComposite(reader, context))
    }
    reader.endArray()
    return list
  }

  override fun serializeComposite(writer: JsonWriter, value: List<T>, context: SerializeCompositeContext) {
    writer.beginArray()
    value.forEach {
      wrappedAdapter.serializeComposite(writer, it, context)
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

  override fun deserializeComposite(reader: JsonReader, context: DeserializeCompositeContext): T? {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      null
    } else {
      wrappedAdapter.deserializeComposite(reader, context)
    }
  }

  override fun serializeComposite(writer: JsonWriter, value: T?, context: SerializeCompositeContext) {
    if (value == null) {
      writer.nullValue()
    } else {
      wrappedAdapter.serializeComposite(writer, value, context)
    }
  }
}

class PresentCompositeAdapter<T>(private val wrappedAdapter: CompositeAdapter<T>) : CompositeAdapter<Optional.Present<@JvmSuppressWildcards T>> {
  override fun deserializeComposite(reader: JsonReader, context: DeserializeCompositeContext): Optional.Present<T> {
    return Optional.Present(wrappedAdapter.deserializeComposite(reader, context))
  }

  override fun serializeComposite(writer: JsonWriter, value: Optional.Present<T>, context: SerializeCompositeContext) {
    wrappedAdapter.serializeComposite(writer, value.value, context)
  }
}

class ApolloOptionalCompositeAdapter<T>(private val wrappedAdapter: CompositeAdapter<T>) : CompositeAdapter<Optional<@JvmSuppressWildcards T>> {
  override fun deserializeComposite(reader: JsonReader, context: DeserializeCompositeContext): Optional<T> {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      Optional.Absent
    } else {
      Optional.Present(wrappedAdapter.deserializeComposite(reader, context))
    }
  }

  override fun serializeComposite(writer: JsonWriter, value: Optional<T>, context: SerializeCompositeContext) {
    if (value is Optional.Present) {
      wrappedAdapter.serializeComposite(writer, value.value, context)
    } else {
      writer.nullValue()
    }
  }
}

class AdapterToCompositeAdapter<T>(private val wrappedAdapter: Adapter<T>) : CompositeAdapter<T> {
  override fun deserializeComposite(reader: JsonReader, context: DeserializeCompositeContext): T {
    return wrappedAdapter.fromJson(reader, CustomScalarAdapters.Empty)
  }

  override fun serializeComposite(writer: JsonWriter, value: T, context: SerializeCompositeContext) {
    wrappedAdapter.toJson(writer, CustomScalarAdapters.Empty, value)
  }
}

class ObjectCompositeAdapter<T>(
    private val wrappedAdapter: CompositeAdapter<T>,
    private val buffered: Boolean,
) : CompositeAdapter<@JvmSuppressWildcards T> {
  override fun deserializeComposite(reader: JsonReader, context: DeserializeCompositeContext): T {
    val actualReader = if (buffered) {
      reader.buffer()
    } else {
      reader
    }
    actualReader.beginObject()
    return wrappedAdapter.deserializeComposite(actualReader, context).also {
      actualReader.endObject()
    }
  }

  override fun serializeComposite(writer: JsonWriter, value: T, context: SerializeCompositeContext) {
    if (buffered && writer !is MapJsonWriter) {
      /**
       * Convert to a Map first
       */
      val mapWriter = MapJsonWriter()
      mapWriter.beginObject()
      wrappedAdapter.serializeComposite(mapWriter, value, context)
      mapWriter.endObject()

      /**
       * And write to the original writer
       */
      writer.writeAny(mapWriter.root()!!)
    } else {
      writer.beginObject()
      wrappedAdapter.serializeComposite(writer, value, context)
      writer.endObject()
    }
  }
}

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
    context: SerializeCompositeContext = SerializeCompositeContext(customScalarAdapters = CustomScalarAdapters.Empty),
    indent: String? = null,
): String = buildJsonString(indent) {
  this@toJsonString.serializeComposite(this, value, context)
}
