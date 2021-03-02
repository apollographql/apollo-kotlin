package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.ResponseAdapterCache.Companion.DEFAULT
import com.apollographql.apollo3.api.internal.MapResponseParser
import com.apollographql.apollo3.api.internal.StreamResponseParser
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.internal.json.MapJsonReader
import com.apollographql.apollo3.api.internal.json.MapJsonWriter
import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import okio.IOException
import kotlin.jvm.JvmOverloads

/**
 * This file contains extension functions to handle [Operation]. The Operation class is relatively slim on purpose because everything
 * it contains has to be generated for each generated operation which could end up in significantly larger code size.
 *
 * Instead, this files defines extensions function to read/write operation data and variables.
 * By extension, the equivalent fragment functions are also handled here because they are really similar
 */

/**
 * Serializes GraphQL operation response data into its equivalent Json representation.
 * For example:
 * <pre>{@code
 *    {
 *      "data": {
 *        "allPlanets": {
 *          "__typename": "PlanetsConnection",
 *          "planets": [
 *            {
 *              "__typename": "Planet",
 *              "name": "Tatooine",
 *              "surfaceWater": 1.0
 *            }
 *          ]
 *        }
 *      }
 *    }
 * }</pre>
 *
 * @param indent the indentation string to be repeated for each level of indentation in the encoded document. Must be a string
 * containing only whitespace. If [indent] is an empty String the encoded document will be compact. Otherwise the encoded
 * document will be more human-readable.
 * @param [responseAdapterCache] configured instance of GraphQL operation response adapters cache. A global empty instance will be used by default.
 */
@JvmOverloads
fun <D : Operation.Data> Operation<D>.toJson(data: D, indent: String = "", responseAdapterCache: ResponseAdapterCache = DEFAULT): String {
  return try {
    val buffer = Buffer()
    val writer = BufferedSinkJsonWriter(buffer).apply {
      this.indent = indent
    }
    // Do we need to wrap in data?
    writer.beginObject()
    writer.name("data")
    adapter(responseAdapterCache).toResponse(writer, data)
    writer.endObject()
    buffer.readUtf8()
  } catch (e: IOException) {
    throw IllegalStateException(e)
  }
}

/**
 * Parses GraphQL operation raw response from [map] with provided [responseAdapterCache] and returns result [Operation.Data]
 *
 * throws if the Data cannot be parsed
 *
 * @param map: a [Map] representing the response data. It's typically the `data` object of a GraphQL response
 */
fun <D : Operation.Data, M: Map<String, Any?>> Operation<D>.fromJson(
    map: M,
    responseAdapterCache: ResponseAdapterCache = DEFAULT,
): D {
  return adapter(responseAdapterCache).fromResponse(MapJsonReader(map))
}

/**
 * Parses GraphQL operation raw response from [map] with provided [responseAdapterCache] and returns result [Operation.Data]
 *
 * throws if the Data cannot be parsed
 *
 * @param map: a [Map] representing the response data. It's typically the `data` object of a GraphQL response
 */
fun <D : Operation.Data> Operation<D>.fromJson(
    bufferedSource: BufferedSource,
    responseAdapterCache: ResponseAdapterCache = DEFAULT,
): D {
  return adapter(responseAdapterCache).fromResponse(BufferedSourceJsonReader(bufferedSource))
}

/**
 * Parses GraphQL operation raw response from the [source] with provided [responseAdapterCache] and returns result [ApolloResponse]
 *
 * @param source A [BufferedSource] representing the full response. It typically contains a "data" and/or "errors" field
 *
 * This will consume [source] so you don't need to close it and you cannot reuse it.
 */
@JvmOverloads
fun <D : Operation.Data> Operation<D>.fromResponse(
    source: BufferedSource,
    responseAdapterCache: ResponseAdapterCache = DEFAULT
): ApolloResponse<D> {
  return StreamResponseParser.parse(source, this, responseAdapterCache)
}

/**
 * Parses GraphQL operation raw response from the [byteString] with provided [responseAdapterCache] and returns result [ApolloResponse]
 *
 * @param byteString A [ByteString] representing the full response. It typically contains a "data" and/or "errors" field
 */
fun <D : Operation.Data> Operation<D>.fromResponse(
    byteString: ByteString,
    responseAdapterCache: ResponseAdapterCache = DEFAULT
): ApolloResponse<D> {
  return fromResponse(Buffer().write(byteString), responseAdapterCache)
}

/**
 * Parses GraphQL operation raw response from the [string] with provided [responseAdapterCache] and returns result [ApolloResponse]
 *
 * @param string A [String] representing the full response. It typically contains a "data" and/or "errors" field
 */
fun <D : Operation.Data> Operation<D>.fromResponse(
    string: String,
    responseAdapterCache: ResponseAdapterCache = DEFAULT
): ApolloResponse<D> {
  return fromResponse(Buffer().writeUtf8(string), responseAdapterCache)
}

/**
 * Parses GraphQL operation raw response from [map] with provided [responseAdapterCache] and returns result [ApolloResponse]
 *
 * @param map: a [Map] representing the response. It typically include a "data" field
 */
fun <D : Operation.Data> Operation<D>.fromResponse(
    map: Map<String, Any?>,
    responseAdapterCache: ResponseAdapterCache = DEFAULT
): ApolloResponse<D> {
  return MapResponseParser.parse(map, this, responseAdapterCache)
}

/**
 * Serializes variables to a Json Map
 */
fun <D : Operation.Data> Operation<D>.variables(responseAdapterCache: ResponseAdapterCache): Operation.Variables {
  val valueMap = MapJsonWriter().apply {
    serializeVariables(this, responseAdapterCache)
  }.root() as Map<String, Any?>
  return Operation.Variables(valueMap)
}

/**
 * Serializes variables to a Json Map
 */
fun <D : Fragment.Data> Fragment<D>.variables(responseAdapterCache: ResponseAdapterCache): Operation.Variables {
  val valueMap = MapJsonWriter().apply {
    serializeVariables(this, responseAdapterCache)
  }.root() as Map<String, Any?>
  return Operation.Variables(valueMap)
}

/**
 * Serializes variables to a Json String
 */
fun <D : Operation.Data> Operation<D>.variablesJson(responseAdapterCache: ResponseAdapterCache): String {
  return Buffer().apply {
    serializeVariables(BufferedSinkJsonWriter(this), responseAdapterCache)
  }.readUtf8()
}

/**
 * Serializes variables to a Json String
 */
fun <D : Fragment.Data> Fragment<D>.variablesJson(responseAdapterCache: ResponseAdapterCache): String {
  return Buffer().apply {
    serializeVariables(BufferedSinkJsonWriter(this), responseAdapterCache)
  }.readUtf8()
}
