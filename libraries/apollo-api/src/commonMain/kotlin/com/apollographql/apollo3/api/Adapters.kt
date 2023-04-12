@file:JvmName("Adapters")

package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.ApolloAdapter.DataDeserializeContext
import com.apollographql.apollo3.api.ApolloAdapter.DataSerializeContext
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
 * This file contains a list of [ApolloAdapter] for standard types
 *
 * They are mostly used from the generated code but could be useful in any other situations that requires adapting from
 * GraphQL to Kotlin.
 * In particular, [AnyApolloAdapter] can be used to read/write a Kotlin representation from/to Json.
 */
class ListAdapter<T>(private val wrappedAdapter: ApolloAdapter<T>) : ApolloAdapter<List<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): List<T> {
    reader.beginArray()
    val list = mutableListOf<T>()
    while (reader.hasNext()) {
      list.add(wrappedAdapter.fromJson(reader, context))
    }
    reader.endArray()
    return list
  }

  override fun toJson(writer: JsonWriter, value: List<T>, context: DataSerializeContext) {
    writer.beginArray()
    value.forEach {
      wrappedAdapter.toJson(writer, it, context)
    }
    writer.endArray()
  }
}

class NullableAdapter<T : Any>(private val wrappedAdapter: ApolloAdapter<T>) : ApolloAdapter<@JvmSuppressWildcards T?> {
  init {
    check(wrappedAdapter !is NullableAdapter<*>) {
      "The adapter is already nullable"
    }
  }

  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): T? {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      null
    } else {
      wrappedAdapter.fromJson(reader, context)
    }
  }

  override fun toJson(writer: JsonWriter, value: T?, context: DataSerializeContext) {
    if (value == null) {
      writer.nullValue()
    } else {
      wrappedAdapter.toJson(writer, value, context)
    }
  }
}

@Deprecated("Use PresentAdapter instead")
class OptionalAdapter<T>(private val wrappedAdapter: ApolloAdapter<T>) : ApolloAdapter<Optional.Present<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): Optional.Present<T> {
    return Optional.Present(wrappedAdapter.fromJson(reader, context))
  }

  override fun toJson(writer: JsonWriter, value: Optional.Present<T>, context: DataSerializeContext) {
    wrappedAdapter.toJson(writer, value.value, context)
  }
}

/**
 * PresentAdapter can only express something that's present. Absent values are handled outside of the adapter.
 *
 * This adapter is used to handle optional arguments in operations and optional fields in Input objects.
 */
class PresentAdapter<T>(private val wrappedAdapter: ApolloAdapter<T>) : ApolloAdapter<Optional.Present<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): Optional.Present<T> {
    return Optional.Present(wrappedAdapter.fromJson(reader, context))
  }

  override fun toJson(writer: JsonWriter, value: Optional.Present<T>, context: DataSerializeContext) {
    wrappedAdapter.toJson(writer, value.value, context)
  }
}


/**
 * This adapter is used to handle nullable fields when they are represented as [Optional].
 * `null` is deserialized as [Optional.Absent].
 */
class ApolloOptionalAdapter<T>(private val wrappedAdapter: ApolloAdapter<T>) : ApolloAdapter<Optional<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): Optional<T> {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      Optional.Absent
    } else {
      Optional.Present(wrappedAdapter.fromJson(reader, context))
    }
  }

  override fun toJson(writer: JsonWriter, value: Optional<T>, context: DataSerializeContext) {
    if (value is Optional.Present) {
      wrappedAdapter.toJson(writer, value.value, context)
    } else {
      writer.nullValue()
    }
  }
}

@JvmField
val StringApolloAdapter = object : ApolloAdapter<String> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): String {
    return reader.nextString()!!
  }

  override fun toJson(writer: JsonWriter, value: String, context: DataSerializeContext) {
    writer.value(value)
  }
}

@JvmField
val IntApolloAdapter = object : ApolloAdapter<Int> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): Int {
    return reader.nextInt()
  }

  override fun toJson(writer: JsonWriter, value: Int, context: DataSerializeContext) {
    writer.value(value)
  }
}

@JvmField
val DoubleApolloAdapter = object : ApolloAdapter<Double> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): Double {
    return reader.nextDouble()
  }

  override fun toJson(writer: JsonWriter, value: Double, context: DataSerializeContext) {
    writer.value(value)
  }
}

/**
 * An [ApolloAdapter] that converts to/from a [Float]
 * Floats are not part of the GraphQL spec but this can be used in custom scalars
 */
@JvmField
val FloatApolloAdapter = object : ApolloAdapter<Float> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): Float {
    return reader.nextDouble().toFloat()
  }

  override fun toJson(writer: JsonWriter, value: Float, context: DataSerializeContext) {
    writer.value(value.toDouble())
  }
}

/**
 * An [ApolloAdapter] that converts to/from a [Long]
 * Longs are not part of the GraphQL spec but this can be used in custom scalars
 *
 * If the Json number does not fit in a [Long], an exception will be thrown
 */
@JvmField
val LongApolloAdapter = object : ApolloAdapter<Long> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): Long {
    return reader.nextLong()
  }

  override fun toJson(writer: JsonWriter, value: Long, context: DataSerializeContext) {
    writer.value(value)
  }
}

@JvmField
val BooleanApolloAdapter = object : ApolloAdapter<Boolean> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): Boolean {
    return reader.nextBoolean()
  }

  override fun toJson(writer: JsonWriter, value: Boolean, context: DataSerializeContext) {
    writer.value(value)
  }
}

@JvmField
val AnyApolloAdapter = object : ApolloAdapter<Any> {
  fun fromJson(reader: JsonReader): Any {
    return reader.readAny()!!
  }

  fun toJson(writer: JsonWriter, value: Any) {
    writer.writeAny(value)
  }

  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): Any {
    return fromJson(reader)
  }

  override fun toJson(writer: JsonWriter, value: Any, context: DataSerializeContext) {
    toJson(writer, value)
  }
}

internal class PassThroughAdapter<T> : ApolloAdapter<T> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): T {
    check(reader is MapJsonReader) {
      "UnsafeAdapter only supports MapJsonReader"
    }

    @Suppress("UNCHECKED_CAST")
    return reader.nextValue() as T
  }

  override fun toJson(writer: JsonWriter, value: T, context: DataSerializeContext) {
    check(writer is MapJsonWriter) {
      "UnsafeAdapter only supports MapJsonWriter"
    }

    writer.value(value)
  }
}

class ScalarAdapterToApolloAdapter<T>(private val wrappedScalarAdapter: ScalarAdapter<T>) : ApolloAdapter<T> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): T {
    return wrappedScalarAdapter.fromJson(reader)
  }

  override fun toJson(writer: JsonWriter, value: T, context: DataSerializeContext) {
    wrappedScalarAdapter.toJson(writer, value)
  }
}

@JvmField
val UploadApolloAdapter = object : ApolloAdapter<Upload> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): Upload {
    error("File Upload used in output position")
  }

  override fun toJson(writer: JsonWriter, value: Upload, context: DataSerializeContext) {
    writer.value(value)
  }
}

/*
 * Global instances of nullable adapters for built-in scalar types
 */
@JvmField
val NullableStringApolloAdapter = StringApolloAdapter.nullable()

@JvmField
val NullableDoubleApolloAdapter = DoubleApolloAdapter.nullable()

@JvmField
val NullableIntApolloAdapter = IntApolloAdapter.nullable()

@JvmField
val NullableBooleanApolloAdapter = BooleanApolloAdapter.nullable()

@JvmField
val NullableAnyApolloAdapter = AnyApolloAdapter.nullable()

/*
 * Global instances of optional adapters for built-in scalar types
 */
@JvmField
val ApolloOptionalStringApolloAdapter = ApolloOptionalAdapter(StringApolloAdapter)

@JvmField
val ApolloOptionalDoubleApolloAdapter = ApolloOptionalAdapter(DoubleApolloAdapter)

@JvmField
val ApolloOptionalIntApolloAdapter = ApolloOptionalAdapter(IntApolloAdapter)

@JvmField
val ApolloOptionalBooleanApolloAdapter = ApolloOptionalAdapter(BooleanApolloAdapter)

@JvmField
val ApolloOptionalAnyApolloAdapter = ApolloOptionalAdapter(AnyApolloAdapter)


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
 * An [ApolloAdapter] that converts to/from a [Float]
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
 * An [ApolloAdapter] that converts to/from a [Long]
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
    private val wrappedAdapter: ApolloAdapter<T>,
    private val buffered: Boolean,
) : ApolloAdapter<@JvmSuppressWildcards T> {
  override fun fromJson(reader: JsonReader, context: DataDeserializeContext): T {
    val actualReader = if (buffered) {
      reader.buffer()
    } else {
      reader
    }
    actualReader.beginObject()
    return wrappedAdapter.fromJson(actualReader, context).also {
      actualReader.endObject()
    }
  }

  override fun toJson(writer: JsonWriter, value: T, context: DataSerializeContext) {
    if (buffered && writer !is MapJsonWriter) {
      /**
       * Convert to a Map first
       */
      val mapWriter = MapJsonWriter()
      mapWriter.beginObject()
      wrappedAdapter.toJson(mapWriter, value, context)
      mapWriter.endObject()

      /**
       * And write to the original writer
       */
      writer.writeAny(mapWriter.root()!!)
    } else {
      writer.beginObject()
      wrappedAdapter.toJson(writer, value, context)
      writer.endObject()
    }
  }
}

@JvmName("-nullable")
fun <T : Any> ApolloAdapter<T>.nullable() = NullableAdapter(this)

@JvmName("-list")
fun <T> ApolloAdapter<T>.list() = ListAdapter(this)

@JvmName("-obj")
fun <T> ApolloAdapter<T>.obj(buffered: Boolean = false) = ObjectAdapter(this, buffered)

@JvmName("-optional")
@Deprecated("Use present instead", ReplaceWith("present()"))
fun <T> ApolloAdapter<T>.optional() = PresentAdapter(this)

@JvmName("-present")
fun <T> ApolloAdapter<T>.present() = PresentAdapter(this)


@JvmName("-toJson")
@JvmOverloads
fun <T> ApolloAdapter<T>.toJsonString(
    value: T,
    context: DataSerializeContext = DataSerializeContext(scalarAdapters = ScalarAdapters.Empty),
    indent: String? = null,
): String = buildJsonString(indent) {
  this@toJsonString.toJson(this, value, context)
}

@JvmName("-toJson")
@JvmOverloads
fun <T> ScalarAdapter<T>.toJsonString(
    value: T,
    indent: String? = null,
): String = buildJsonString(indent) {
  this@toJsonString.toJson(this, value)
}
