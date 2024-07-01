@file:JvmName("Adapters")

package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.api.json.MapJsonReader
import com.apollographql.apollo.api.json.MapJsonReader.Companion.buffer
import com.apollographql.apollo.api.json.MapJsonWriter
import com.apollographql.apollo.api.json.buildJsonString
import com.apollographql.apollo.api.json.readAny
import com.apollographql.apollo.api.json.writeAny
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloGraphQLException
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

@Deprecated("Use PresentAdapter instead")
class OptionalAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<Optional.Present<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Optional.Present<T> {
    return Optional.Present(wrappedAdapter.fromJson(reader, customScalarAdapters))
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Optional.Present<T>) {
    wrappedAdapter.toJson(writer, customScalarAdapters, value.value)
  }
}

/**
 * PresentAdapter can only express something that's present. Absent values are handled outside the adapter.
 *
 * This adapter is used to handle optional arguments in operations and optional fields in Input objects.
 */
class PresentAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<Optional.Present<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Optional.Present<T> {
    return Optional.Present(wrappedAdapter.fromJson(reader, customScalarAdapters))
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Optional.Present<T>) {
    wrappedAdapter.toJson(writer, customScalarAdapters, value.value)
  }
}


/**
 * This adapter is used to handle nullable fields when they are represented as [Optional].
 * `null` is deserialized as [Optional.Absent].
 */
class ApolloOptionalAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<Optional<@JvmSuppressWildcards T>> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Optional<T> {
    return if (reader.peek() == JsonReader.Token.NULL) {
      reader.skipValue()
      Optional.Absent
    } else {
      Optional.Present(wrappedAdapter.fromJson(reader, customScalarAdapters))
    }
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Optional<T>) {
    if (value is Optional.Present) {
      wrappedAdapter.toJson(writer, customScalarAdapters, value.value)
    } else {
      writer.nullValue()
    }
  }
}

@JvmField
val StringAdapter = object : Adapter<String> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): String {
    return reader.nextString()!!
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: String) {
    writer.value(value)
  }
}

@JvmField
val IntAdapter = object : Adapter<Int> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Int {
    return reader.nextInt()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Int) {
    writer.value(value)
  }
}

@JvmField
val DoubleAdapter = object : Adapter<Double> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Double {
    return reader.nextDouble()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Double) {
    writer.value(value)
  }
}

/**
 * An [Adapter] that converts to/from a [Float]
 * Floats are not part of the GraphQL spec but this can be used in custom scalars
 */
@JvmField
val FloatAdapter = object : Adapter<Float> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Float {
    return reader.nextDouble().toFloat()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Float) {
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
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Long {
    return reader.nextLong()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Long) {
    writer.value(value)
  }
}

@JvmField
val BooleanAdapter = object : Adapter<Boolean> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Boolean {
    return reader.nextBoolean()
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Boolean) {
    writer.value(value)
  }
}

@OptIn(ApolloInternal::class)
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

internal class PassThroughAdapter<T> : Adapter<T> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T {
    check(reader is MapJsonReader) {
      "UnsafeAdapter only supports MapJsonReader"
    }

    @Suppress("UNCHECKED_CAST")
    return reader.nextValue() as T
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T) {
    check(writer is MapJsonWriter) {
      "UnsafeAdapter only supports MapJsonWriter"
    }

    writer.value(value)
  }
}

@JvmField
val UploadAdapter = object : Adapter<Upload> {
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

/**
 * Note that Arrays require their type to be known at compile time, so we construct an anonymous object with reference to
 * function with reified type parameters as a workaround.
 *
 */
@JvmName("-array")
inline fun <reified T> Adapter<T>.array() = object : Adapter<Array<T>> {

  private inline fun <reified T> arrayFromJson(
      wrappedAdapter: Adapter<T>,
      reader: JsonReader,
      customScalarAdapters: CustomScalarAdapters,
  ): Array<T> {
    reader.beginArray()
    val list = mutableListOf<T>()
    while (reader.hasNext()) {
      list.add(wrappedAdapter.fromJson(reader, customScalarAdapters))
    }
    reader.endArray()
    return list.toTypedArray()
  }

  private inline fun <reified T> arrayToJson(
      wrappedAdapter: Adapter<T>,
      writer: JsonWriter,
      customScalarAdapters: CustomScalarAdapters,
      value: Array<T>,
  ) {
    writer.beginArray()
    value.forEach {
      wrappedAdapter.toJson(writer, customScalarAdapters, it)
    }
    writer.endArray()
  }

  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Array<T> {
    return arrayFromJson(this@array, reader, customScalarAdapters)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Array<T>) {
    return arrayToJson(this@array, writer, customScalarAdapters, value)
  }
}

@JvmName("-optional")
@Deprecated("Use present instead", ReplaceWith("present()"))
fun <T> Adapter<T>.optional() = PresentAdapter(this)

@JvmName("-present")
fun <T> Adapter<T>.present() = PresentAdapter(this)


@JvmName("-toJson")
@JvmOverloads
fun <T> Adapter<T>.toJsonString(
    value: T,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    indent: String? = null,
): String = buildJsonString(indent) {
  this@toJsonString.toJson(this, customScalarAdapters, value)
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
      writer.writeAny(mapWriter.root()!!)
    } else {
      writer.beginObject()
      wrappedAdapter.toJson(writer, customScalarAdapters, value)
      writer.endObject()
    }
  }
}

private class ErrorAwareAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<T> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T {
    if (reader.peek() == JsonReader.Token.NULL) {
      val error = customScalarAdapters.firstErrorStartingWith(reader.getPath())
      if (error != null) {
        reader.skipValue()
        throw ApolloGraphQLException(error)
      }
    }

    return wrappedAdapter.fromJson(reader, customScalarAdapters)
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T) {
    wrappedAdapter.toJson(writer, customScalarAdapters, value)
  }
}

private class CatchToResultAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<FieldResult<T>> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): FieldResult<T> {
    return try {
      FieldResult.Success(wrappedAdapter.fromJson(reader, customScalarAdapters))
    } catch (e: ApolloException) {
      FieldResult.Failure(e)
    }
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: FieldResult<T>) {
    when (value) {
      is FieldResult.Success -> wrappedAdapter.toJson(writer, customScalarAdapters, value.getOrThrow())
      else -> Unit // ignore errors
    }
  }
}

private class CatchToNullAdapter<T>(private val wrappedAdapter: Adapter<T>) : Adapter<@JvmSuppressWildcards T?> {
  override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T? {
    return try {
      wrappedAdapter.fromJson(reader, customScalarAdapters)
    } catch (e: ApolloException) {
      null
    }
  }

  override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T?) {
    if (value == null) {
      // XXX: this potentially writes null instead of an error
      writer.nullValue()
    } else {
      wrappedAdapter.toJson(writer, customScalarAdapters, value)
    }
  }
}

@JvmName("-nullable")
fun <T : Any> Adapter<T>.nullable() = NullableAdapter(this)

@JvmName("-list")
fun <T> Adapter<T>.list() = ListAdapter(this)

@JvmName("-obj")
fun <T> Adapter<T>.obj(buffered: Boolean = false) = ObjectAdapter(this, buffered)

@JvmName("-catchToResult")
fun <T> Adapter<T>.catchToResult(): Adapter<FieldResult<T>> = CatchToResultAdapter(this)

@JvmName("-errorAware")
fun <T> Adapter<T>.errorAware(): Adapter<T> = ErrorAwareAdapter(this)

@JvmName("-catchToNull")
fun <T> Adapter<T>.catchToNull(): Adapter<T?> = CatchToNullAdapter(this)

/**
 * A replica of Apollo Android v2's CustomTypeAdapter, to ease migration from v2 to v3.
 *
 * Make your CustomTypeAdapters implement this interface by updating the imports
 * from `com.apollographql.apollo.api` to `com.apollographql.apollo.api`.
 *
 * **Note**: [Adapter]s are called from multiple threads and implementations must be thread safe.
 */
@Deprecated("CustomTypeAdapter was used for backward compatibility with 2.x. Use Adapter instead", level = DeprecationLevel.ERROR)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_0_0)
interface CustomTypeAdapter<T>