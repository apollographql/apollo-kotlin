package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.internal.json.MapJsonReader
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.JsonWriter
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource

/**
 * A [ResponseAdapter] is responsible for adapting GraphQL types to Kotlin types.
 *
 * It is used to
 * - deserialize network responses
 * - serialize variables
 * - normalize models into records that can be stored in cache
 * - deserialize records
 */
interface ResponseAdapter<T> {
  fun fromResponse(reader: JsonReader, responseAdapterCache: ResponseAdapterCache): T

  /**
   * Serializes a GraphQL model into its equivalent Json representation.
   * For example:
   * <pre>{@code
   *    {
   *      "allPlanets": {
   *        "__typename": "PlanetsConnection",
   *        "planets": [
   *          {
   *            "__typename": "Planet",
   *            "name": "Tatooine",
   *            "surfaceWater": 1.0
   *          }
   *        ]
   *      }
   *    }
   * }</pre>
   *
   * @param [responseAdapterCache] configured instance of GraphQL operation response adapters cache. A global empty instance will be used by default.
   */
  fun toResponse(writer: JsonWriter, responseAdapterCache: ResponseAdapterCache, value: T)
}

fun <T> ResponseAdapter<T>.toJson(
    value: T,
    responseAdapterCache: ResponseAdapterCache = ResponseAdapterCache.DEFAULT,
    indent: String = "",
): String {
  val buffer = Buffer()

  toJson(buffer, value, responseAdapterCache, indent)
  return buffer.readUtf8()
}

fun <T> ResponseAdapter<T>.toJson(
    sink: BufferedSink,
    value: T,
    responseAdapterCache: ResponseAdapterCache = ResponseAdapterCache.DEFAULT,
    indent: String = "",
) {
  val writer = BufferedSinkJsonWriter(sink)
  writer.indent = indent

  toResponse(writer, responseAdapterCache, value)
}

fun <T> ResponseAdapter<T>.fromJson(
    bufferedSource: BufferedSource,
    responseAdapterCache: ResponseAdapterCache = ResponseAdapterCache.DEFAULT,
): T {
  return fromResponse(BufferedSourceJsonReader(bufferedSource), responseAdapterCache)
}

fun <T> ResponseAdapter<T>.fromJson(
    string: String,
    responseAdapterCache: ResponseAdapterCache = ResponseAdapterCache.DEFAULT,
): T {
  return fromJson(Buffer().apply { writeUtf8(string) }, responseAdapterCache)
}

fun <T, M : Map<String, Any?>> ResponseAdapter<T>.fromMap(
    map: M,
    responseAdapterCache: ResponseAdapterCache = ResponseAdapterCache.DEFAULT,
): T {
  return fromResponse(MapJsonReader(map), responseAdapterCache)
}

