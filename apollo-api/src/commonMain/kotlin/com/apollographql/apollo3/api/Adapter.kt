package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource

/**
 * An [Adapter] is responsible for adapting Kotlin-generated GraphQL types to/from their Json representation.
 *
 * It is used to
 * - deserialize network responses
 * - serialize variables
 * - normalize models into records that can be stored in cache
 * - deserialize records
 *
 * For an example, a `Hero` GraphQL type can be represented as:
 *
 * ```
 * {
 *   "name": "Luke Skywalker",
 *   "homeworld": "Tatooine"
 * }
 * ```
 */
interface Adapter<T> {
  /**
   * Deserializes the given Json to the expected Kotlin type.
   *
   * implementations may throw [com.apollographql.apollo3.exception.JsonEncodingException] or [com.apollographql.apollo3.exception.JsonDataException]
   * on unexpected incoming data
   *
   * @param [customScalarAdapters] configured instance of GraphQL operation response adapters cache. A global empty instance will be used by default.
   *
   * Example:
   * ```
   * override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Hero {
   *   var name: String? = null
   *   var homeworld: String? = null
   *
   *   while (reader.hasNext()) {
   *     when(reader.nextName()) {
   *       "name" -> name = reader.nextString()
   *       "homeworld" -> homeworld = reader.nextString()
   *     }
   *   }
   *
   *   return Hero(name!!, homeworld!!)
   * }
   * ```
   *
   * Alternatively, you can use the the built-in [AnyAdapter] to simplify the parsing loop:
   * ```
   * override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Hero {
   *   val map = AnyAdapter.fromResponse(reader) as Map<String, String>
   *
   *   return Hero(map["name"]!!, map["homeworld"]!!)
   * }
   * ```
   */
  fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): T

  /**
   * Serializes a Kotlin type into its equivalent Json representation.
   *
   * Example:
   * ```
   * override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Hero) {
   *   writer.name("name")
   *   writer.value(value.name)
   *   writer.name("homeworld")
   *   writer.value(value.homeworld)
   * }
   *```
   *
   * Alternatively, you can use the the built-in [AnyAdapter]:
   * ```
   * override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Hero) {
   *   val map = mapOf("name" to value.name, "homeworld" to value.homeworld)
   *   AnyAdapter.toJson(writer, customScalarAdapters, map)
   * }
   * ```
   */
  fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T)
}

fun <T> Adapter<T>.toJson(
    value: T,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    indent: String = "",
): String {
  val buffer = Buffer()

  toJson(buffer, value, customScalarAdapters, indent)
  return buffer.readUtf8()
}

fun <T> Adapter<T>.toJson(
    sink: BufferedSink,
    value: T,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    indent: String = "",
) {
  val writer = BufferedSinkJsonWriter(sink)
  writer.indent = indent

  toJson(writer, customScalarAdapters, value)
}

fun <T> Adapter<T>.fromJson(
    bufferedSource: BufferedSource,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): T {
  return fromJson(BufferedSourceJsonReader(bufferedSource), customScalarAdapters)
}

fun <T> Adapter<T>.fromJson(
    string: String,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): T {
  return fromJson(Buffer().apply { writeUtf8(string) }, customScalarAdapters)
}

fun <T, M : Map<String, Any?>> Adapter<T>.fromMap(
    map: M,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
): T {
  return fromJson(MapJsonReader(map), customScalarAdapters)
}
