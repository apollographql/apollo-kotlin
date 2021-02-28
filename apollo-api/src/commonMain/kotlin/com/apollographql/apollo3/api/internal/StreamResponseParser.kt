package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ResponseAdapter
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Throws
import com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.internal.json.Utils.readRecursively
import com.apollographql.apollo3.api.json.use
import com.benasher44.uuid.uuid4
import okio.BufferedSource
import okio.IOException
import kotlin.jvm.JvmStatic

/**
 * [StreamResponseParser] parses network responses, including data, errors and extensions from a [JsonReader]
 *
 * That will avoid the cost of having to create an entire Map in memory
 */
object StreamResponseParser {
  @JvmStatic
  @Throws(IOException::class)
  fun <D : Operation.Data> parse(
      source: BufferedSource,
      operation: Operation<D>,
      responseAdapterCache: ResponseAdapterCache
  ): ApolloResponse<D> {
    return BufferedSourceJsonReader(source).use { jsonReader ->
      jsonReader.beginObject()

      var data: D? = null
      var errors: List<Error>? = null
      var extensions: Map<String, Any?>? = null
      while (jsonReader.hasNext()) {
        when (jsonReader.nextName()) {
          "data" -> data = jsonReader.readData(
              adapter = operation.adapter(responseAdapterCache),
          )
          "errors" -> errors = jsonReader.readErrors()
          "extensions" -> extensions = jsonReader.readRecursively() as Map<String, Any?>
          else -> jsonReader.skipValue()
        }
      }

      jsonReader.endObject()

      ApolloResponse(
          requestUuid = uuid4(),
          operation = operation,
          data = data,
          errors = errors,
          extensions = extensions.orEmpty()
      )
    }
  }

  private fun <D : Operation.Data> JsonReader.readData(
      adapter: ResponseAdapter<D>,
  ): D? {
    if (peek() == JsonReader.Token.NULL) {
      return nextNull<D>()
    }

    return adapter.fromResponse(this)
  }

  @Suppress("UNCHECKED_CAST")
  private fun JsonReader.readErrors(): List<Error> {
    val responseErrors = readRecursively() as? List<Map<String, Any?>>
    return responseErrors?.let {
      it.map { errorPayload -> errorPayload.readError() }
    }.orEmpty()
  }

  @Suppress("UNCHECKED_CAST")
  private fun Map<String, Any?>.readError(): Error {
    var message = ""
    var locations = emptyList<Error.Location>()
    val customAttributes = mutableMapOf<String, Any?>()
    for ((key, value) in this) {
      when (key) {
        "message" -> message = value?.toString() ?: ""
        "locations" -> {
          val locationItems = value as? List<Map<String, Any?>>
          locations = locationItems?.map { it.readErrorLocation() } ?: emptyList()
        }
        else -> customAttributes[key] = value
      }
    }
    return Error(message, locations, customAttributes)
  }

  private fun Map<String, Any?>?.readErrorLocation(): Error.Location {
    var line: Long = -1
    var column: Long = -1
    if (this != null) {
      for ((key, value) in this) {
        when (key) {
          "line" -> line = (value as Number).toLong()
          "column" -> column = (value as Number).toLong()
        }
      }
    }
    return Error.Location(line, column)
  }
}
