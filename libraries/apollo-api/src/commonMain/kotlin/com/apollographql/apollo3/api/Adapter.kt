package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import okio.IOException

/**
 * An [Adapter] is responsible for adapting scalars between their Json and Kotlin representations.
 *
 * It is used to
 * - deserialize network responses
 * - serialize variables
 * - normalize models into records that can be stored in cache
 * - deserialize records
 *
 * **Note**: [Adapter]s are called from multiple threads and implementations must be thread safe.
 */
interface Adapter<T> {
  /**
   * Deserializes the given Json to the expected Kotlin type.
   *
   * implementations may throw [com.apollographql.apollo3.exception.JsonEncodingException] or [com.apollographql.apollo3.exception.JsonDataException]
   * on unexpected incoming data
   *
   * Example:
   * ```
   * override fun fromJson(reader: JsonReader): LocalDateTime {
   *   return LocalDateTime.parse(reader.nextString()!!)
   * }
   * ```
   *
   * Alternatively, you can use the built-in [AnyAdapter] to simplify the parsing loop:
   * ```
   * override fun fromJson(reader: JsonReader): GeoPoint {
   *   val map = AnyAdapter.fromJson(reader) as Map<String, Double>
   *
   *   return GeoPoint(map["lat"]!!, map["lon"]!!)
   * }
   * ```
   */
  @Throws(IOException::class)
  fun fromJson(reader: JsonReader, customScalarAdapters: ScalarAdapters): T

  /**
   * Serializes a Kotlin type into its equivalent Json representation.
   *
   * Example:
   * ```
   * override fun toJson(writer: JsonWriter value: LocalDateTime) {
   *   writer.value(value.toString())
   * }
   * ```
   *
   * Alternatively, you can use the built-in [AnyAdapter]:
   * ```
   * override fun toJson(writer: JsonWriter, value: GeoPoint) {
   *   val map = mapOf("lat" to value.lat, "lon" to value.lon)
   *   AnyAdapter.toJson(writer, map)
   * }
   * ```
   */
  @Throws(IOException::class)
  fun toJson(writer: JsonWriter, customScalarAdapters: ScalarAdapters, value: T)
}
