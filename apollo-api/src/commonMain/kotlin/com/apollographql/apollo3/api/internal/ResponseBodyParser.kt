package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.internal.json.MapJsonReader
import com.apollographql.apollo3.api.internal.json.Utils.readRecursively
import com.apollographql.apollo3.api.json.JsonReader
import com.apollographql.apollo3.api.json.use
import com.apollographql.apollo3.api.nullable
import com.benasher44.uuid.uuid4
import okio.BufferedSource

/**
 * [ResponseBodyParser] parses network responses, including data, errors and extensions from a [JsonReader]
 */
object ResponseBodyParser {
  fun <D : Operation.Data> parse(
      jsonReader: JsonReader,
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters
  ): ApolloResponse<D> {
    jsonReader.beginObject()

    var data: D? = null
    var errors: List<Error>? = null
    var extensions: Map<String, Any?>? = null
    while (jsonReader.hasNext()) {
      @Suppress("UNCHECKED_CAST")
      when (jsonReader.nextName()) {
        "data" -> data = operation.adapter().nullable().fromJson(jsonReader, customScalarAdapters)
        "errors" -> errors = jsonReader.readErrors()
        "extensions" -> extensions = jsonReader.readRecursively() as Map<String, Any?>
        else -> jsonReader.skipValue()
      }
    }

    jsonReader.endObject()

    return ApolloResponse(
        requestUuid = uuid4(),
        operation = operation,
        data = data,
        errors = errors,
        extensions = extensions.orEmpty()
    )
  }

  fun <D : Operation.Data> parse(
      source: BufferedSource,
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters
  ): ApolloResponse<D> {
    return BufferedSourceJsonReader(source).use { jsonReader ->
      parse(jsonReader, operation, customScalarAdapters)
    }
  }

  fun <D : Operation.Data> parse(
      payload: Map<String, Any?>,
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters,
  ): ApolloResponse<D> {
    return parse(
        MapJsonReader(payload),
        operation = operation,
        customScalarAdapters = customScalarAdapters
    )
  }

  fun parseError(
      payload: Map<String, Any?>,
  ) = payload.readError()

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
    var locations: List<Error.Location>? = null
    var path: List<Any>? = null
    var extensions: Map<String, Any?>? = null
    for ((key, value) in this) {
      when (key) {
        "message" -> message = value?.toString() ?: ""
        "locations" -> {
          val locationItems = value as? List<Map<String, Any?>>
          locations = locationItems?.map { it.readErrorLocation() } 
        }
        "path" -> {
          path = value as? List<Any>
        }
        "extensions" -> {
          extensions = value as? Map<String, Any?>
        }
        else -> {
          // unknown
        }
      }
    }

    return Error(message, locations, path, extensions)
  }

  private fun Map<String, Any?>?.readErrorLocation(): Error.Location {
    var line: Int = -1
    var column: Int = -1
    if (this != null) {
      for ((key, value) in this) {
        when (key) {
          "line" -> line = value as Int
          "column" -> column = value as Int
        }
      }
    }
    return Error.Location(line, column)
  }
}
