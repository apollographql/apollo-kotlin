@file:JvmName("DataAdapters")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.DataAdapter.DeserializeDataContext
import com.apollographql.apollo3.api.DataAdapter.SerializeDataContext
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

@JvmField
val StringDataAdapter = object : DataAdapter<String> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): String {
    return reader.nextString()!!
  }

  override fun serializeData(writer: JsonWriter, value: String, context: SerializeDataContext) {
    writer.value(value)
  }
}

@JvmField
val IntDataAdapter = object : DataAdapter<Int> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): Int {
    return reader.nextInt()
  }

  override fun serializeData(writer: JsonWriter, value: Int, context: SerializeDataContext) {
    writer.value(value)
  }
}

@JvmField
val DoubleDataAdapter = object : DataAdapter<Double> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): Double {
    return reader.nextDouble()
  }

  override fun serializeData(writer: JsonWriter, value: Double, context: SerializeDataContext) {
    writer.value(value)
  }
}

/**
 * A [DataAdapter] that converts to/from a [Float]
 * Floats are not part of the GraphQL spec but this can be used in custom scalars
 */
@JvmField
val FloatDataAdapter = object : DataAdapter<Float> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): Float {
    return reader.nextDouble().toFloat()
  }

  override fun serializeData(writer: JsonWriter, value: Float, context: SerializeDataContext) {
    writer.value(value.toDouble())
  }
}

/**
 * A [DataAdapter] that converts to/from a [Long]
 * Longs are not part of the GraphQL spec but this can be used in custom scalars
 *
 * If the Json number does not fit in a [Long], an exception will be thrown
 */
@JvmField
val LongDataAdapter = object : DataAdapter<Long> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): Long {
    return reader.nextLong()
  }

  override fun serializeData(writer: JsonWriter, value: Long, context: SerializeDataContext) {
    writer.value(value)
  }
}

@JvmField
val BooleanDataAdapter = object : DataAdapter<Boolean> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): Boolean {
    return reader.nextBoolean()
  }

  override fun serializeData(writer: JsonWriter, value: Boolean, context: SerializeDataContext) {
    writer.value(value)
  }
}

@JvmField
val AnyDataAdapter = object : DataAdapter<Any> {
  fun fromJson(reader: JsonReader): Any {
    return reader.readAny()!!
  }

  fun toJson(writer: JsonWriter, value: Any) {
    writer.writeAny(value)
  }

  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): Any {
    return fromJson(reader)
  }

  override fun serializeData(writer: JsonWriter, value: Any, context: SerializeDataContext) {
    toJson(writer, value)
  }
}

internal class PassThroughDataAdapter<T> : DataAdapter<T> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): T {
    check(reader is MapJsonReader) {
      "UnsafeAdapter only supports MapJsonReader"
    }

    @Suppress("UNCHECKED_CAST")
    return reader.nextValue() as T
  }

  override fun serializeData(writer: JsonWriter, value: T, context: SerializeDataContext) {
    check(writer is MapJsonWriter) {
      "UnsafeAdapter only supports MapJsonWriter"
    }

    writer.value(value)
  }
}

class AdapterToDataAdapter<T>(private val wrappedAdapter: Adapter<T>) : DataAdapter<T> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): T {
    return wrappedAdapter.fromJson(reader)
  }

  override fun serializeData(writer: JsonWriter, value: T, context: SerializeDataContext) {
    wrappedAdapter.toJson(writer, value)
  }
}

@JvmField
val UploadDataAdapter = object : DataAdapter<Upload> {
  override fun deserializeData(reader: JsonReader, context: DeserializeDataContext): Upload {
    error("File Upload used in output position")
  }

  override fun serializeData(writer: JsonWriter, value: Upload, context: SerializeDataContext) {
    writer.value(value)
  }
}

/*
 * Global instances of nullable adapters for built-in scalar types
 */
@JvmField
val NullableStringDataAdapter = StringDataAdapter.nullable()

@JvmField
val NullableDoubleDataAdapter = DoubleDataAdapter.nullable()

@JvmField
val NullableIntDataAdapter = IntDataAdapter.nullable()

@JvmField
val NullableBooleanDataAdapter = BooleanDataAdapter.nullable()

@JvmField
val NullableAnyDataAdapter = AnyDataAdapter.nullable()

/*
 * Global instances of optional adapters for built-in scalar types
 */
@JvmField
val ApolloOptionalStringDataAdapter = ApolloOptionalDataAdapter(StringDataAdapter)

@JvmField
val ApolloOptionalDoubleDataAdapter = ApolloOptionalDataAdapter(DoubleDataAdapter)

@JvmField
val ApolloOptionalIntDataAdapter = ApolloOptionalDataAdapter(IntDataAdapter)

@JvmField
val ApolloOptionalBooleanDataAdapter = ApolloOptionalDataAdapter(BooleanDataAdapter)

@JvmField
val ApolloOptionalAnyDataAdapter = ApolloOptionalDataAdapter(AnyDataAdapter)


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
    context: SerializeDataContext = SerializeDataContext(scalarAdapters = ScalarAdapters.Empty),
    indent: String? = null,
): String = buildJsonString(indent) {
  this@toJsonString.serializeData(this, value, context)
}
