package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.ResponseAdapterCache.Companion.DEFAULT
import com.apollographql.apollo3.api.internal.json.MapJsonReader
import com.apollographql.apollo3.api.internal.json.MapJsonWriter
import com.apollographql.apollo3.api.internal.MapResponseParser
import com.apollographql.apollo3.api.internal.OperationRequestBodyComposer
import com.apollographql.apollo3.api.internal.StreamResponseParser
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import okio.IOException
import kotlin.jvm.JvmOverloads

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
 * Composes POST JSON-encoded request body to be sent to the GraphQL server.
 *
 * In case when [autoPersistQueries] is set to `true` special `extension` attributes, required by query auto persistence,
 * will be encoded along with regular GraphQL request body. If query was previously persisted on the GraphQL server
 * set [withQueryDocument] to `false` to skip query document be sent in the request.
 *
 * Optional [responseAdapterCache] must be provided in case when this operation defines variables with custom GraphQL scalar type.
 *
 * *Example*:
 * ```
 * {
 *    "query": "query TestQuery($episode: Episode) { hero(episode: $episode) { name } }",
 *    "operationName": "TestQuery",
 *    "variables": { "episode": "JEDI" }
 *    "extensions": {
 *      "persistedQuery": {
 *        "version": 1,
 *        "sha256Hash": "32637895609e6c51a2593f5cfb49244fd79358d327ff670b3e930e024c3db8f6"
 *      }
 *    }
 * }
 * ```
 */
@JvmOverloads
fun Operation<*>.composeRequestBody(
    autoPersistQueries: Boolean,
    withQueryDocument: Boolean,
    responseAdapterCache: ResponseAdapterCache = DEFAULT
): ByteString {
  return OperationRequestBodyComposer.compose(
      operation = this,
      autoPersistQueries = autoPersistQueries,
      withQueryDocument = withQueryDocument,
      responseAdapterCache = responseAdapterCache
  ).let {
    Buffer().apply {
      it.writeTo(this)
    }.readByteString()
  }
}

@Deprecated("use composeRequestBody instead")
fun Operation<*>.composeRequestBody(
    responseAdapterCache: ResponseAdapterCache = DEFAULT
): ByteString {
  return OperationRequestBodyComposer.compose(
      operation = this,
      autoPersistQueries = false,
      withQueryDocument = true,
      responseAdapterCache = responseAdapterCache
  ).let {
    Buffer().apply {
      it.writeTo(this)
    }.readByteString()
  }
}
/**
 * Parses GraphQL operation raw response from the [source] with provided [responseAdapterCache] and returns result [Response]
 *
 * This will consume [source] so you don't need to close it. Also, you cannot reuse it
 */
@JvmOverloads
fun <D : Operation.Data> Operation<D>.parse(
    source: BufferedSource,
    responseAdapterCache: ResponseAdapterCache = DEFAULT
): Response<D> {
  return StreamResponseParser.parse(source, this, responseAdapterCache)
}

/**
 * Parses GraphQL operation raw response from [byteString] with provided [responseAdapterCache] and returns result [Response]
 */
fun <D : Operation.Data> Operation<D>.parse(
    byteString: ByteString,
    responseAdapterCache: ResponseAdapterCache = DEFAULT
): Response<D> {
  return parse(Buffer().write(byteString), responseAdapterCache)
}

/**
 * Parses GraphQL operation raw response from [string] with provided [responseAdapterCache] and returns result [Response]
 */
fun <D : Operation.Data> Operation<D>.parse(
    string: String,
    responseAdapterCache: ResponseAdapterCache = DEFAULT
): Response<D> {
  return parse(Buffer().writeUtf8(string), responseAdapterCache)
}

/**
 * Parses GraphQL operation raw response from [map] with provided [responseAdapterCache] and returns result [Response]
 *
 * @param map: a [Map] representing the response. It typically include a "data" field
 */
fun <D : Operation.Data> Operation<D>.parse(
    map: Map<String, Any?>,
    responseAdapterCache: ResponseAdapterCache = DEFAULT
): Response<D> {
  return MapResponseParser.parse(map, this, responseAdapterCache)
}

/**
 * Parses GraphQL operation raw response from [map] with provided [responseAdapterCache] and returns result [Response]
 *
 * @param map: a [Map] representing the response. It typically include a "data" field
 */
fun <D : Operation.Data, M: Map<String, Any?>> Operation<D>.parseData(
    map: M,
    responseAdapterCache: ResponseAdapterCache = DEFAULT,
): D {
  return MapJsonReader(
      map,
  ).let {
    adapter(responseAdapterCache).fromResponse(it)
  }
}

fun <D : Operation.Data> Operation<D>.variables(responseAdapterCache: ResponseAdapterCache): Operation.Variables {
  val valueMap = MapJsonWriter().apply {
    serializeVariables(this, responseAdapterCache)
  }.root() as Map<String, Any?>
  return Operation.Variables(valueMap)
}


fun <D : Fragment.Data> Fragment<D>.variables(responseAdapterCache: ResponseAdapterCache): Operation.Variables {
  val valueMap = MapJsonWriter().apply {
    serializeVariables(this, responseAdapterCache)
  }.root() as Map<String, Any?>
  return Operation.Variables(valueMap)
}


fun <D : Operation.Data> Operation<D>.variablesJson(responseAdapterCache: ResponseAdapterCache): String {
  return Buffer().apply {
    serializeVariables(BufferedSinkJsonWriter(this), responseAdapterCache)
  }.readUtf8()
}


fun <D : Fragment.Data> Fragment<D>.variablesJson(responseAdapterCache: ResponseAdapterCache): String {
  return Buffer().apply {
    serializeVariables(BufferedSinkJsonWriter(this), responseAdapterCache)
  }.readUtf8()
}
