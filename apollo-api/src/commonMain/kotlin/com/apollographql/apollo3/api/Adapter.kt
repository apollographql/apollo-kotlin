package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import okio.IOException

/**
 * An [Adapter] is responsible for adapting Kotlin-generated GraphQL types to/from their Json representation.
 *
 * It is used to
 * - deserialize network responses
 * - serialize variables
 * - normalize models into records that can be stored in cache
 * - deserialize records
 *
 * **Note**: [Adapter]s are called from multiple threads and implementations must be thread safe.
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
   * Alternatively, you can use the built-in [AnyAdapter] to simplify the parsing loop:
   * ```
   * override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters): Hero {
   *   val map = AnyAdapter.fromJson(reader) as Map<String, String>
   *
   *   return Hero(map["name"]!!, map["homeworld"]!!)
   * }
   * ```
   */
  @Throws(IOException::class)
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
   * Alternatively, you can use the built-in [AnyAdapter]:
   * ```
   * override fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: Hero) {
   *   val map = mapOf("name" to value.name, "homeworld" to value.homeworld)
   *   AnyAdapter.toJson(writer, customScalarAdapters, map)
   * }
   * ```
   */
  @Throws(IOException::class)
  fun toJson(writer: JsonWriter, customScalarAdapters: CustomScalarAdapters, value: T)
}
