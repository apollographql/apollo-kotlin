package com.apollographql.apollo3.api.internal

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.json.MapJsonReader
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
        "extensions" -> extensions = jsonReader.readRecursively() as? Map<String, Any?>
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
  ) = MapJsonReader(payload).readError()

  @Suppress("UNCHECKED_CAST")
  private fun JsonReader.readErrors(): List<Error> {
    if (peek() == JsonReader.Token.NULL) {
      nextNull()
      return emptyList()
    }

    beginArray()
    val list = mutableListOf<Error>()
    while(hasNext()) {
      list.add(readError())
    }
    endArray()
    return list
  }

  @Suppress("UNCHECKED_CAST")
  private fun JsonReader.readError(): Error {
    var message = ""
    var locations: List<Error.Location>? = null
    var path: List<Any>? = null
    var extensions: Map<String, Any?>? = null
    beginObject()
    while (hasNext()) {
      when (nextName()) {
        "message" -> message = nextString() ?: ""
        "locations" -> {
          locations = readErrorLocations()
        }
        "path" -> {
          path = readPath()
        }
        "extensions" -> {
          extensions = readRecursively() as? Map<String, Any?>?
        }
        else -> skipValue()
      }
    }
    endObject()

    return Error(message, locations, path, extensions)
  }

  private fun JsonReader.readPath(): List<Any>? {
    if (peek() == JsonReader.Token.NULL) {
      return nextNull()
    }

    val list = mutableListOf<Any>()
    beginArray()
    while(hasNext()) {
      when (peek()) {
        JsonReader.Token.NUMBER, JsonReader.Token.LONG -> list.add(nextInt())
        else -> list.add(nextString()!!)
      }
    }
    endArray()

    return list
  }
  private fun JsonReader.readErrorLocations(): List<Error.Location>? {
    if (peek() == JsonReader.Token.NULL) {
      return nextNull()
    }
    val list = mutableListOf<Error.Location>()
    beginArray()
    while(hasNext()) {
      list.add(readErrorLocation())
    }
    endArray()

    return list
  }

  private fun JsonReader.readErrorLocation(): Error.Location {
    var line: Int = -1
    var column: Int = -1
    beginObject()
    while(hasNext()) {
      when (nextName()) {
        "line" -> line = nextInt()
        "column" -> column = nextInt()
        else -> skipValue()
      }
    }
    endObject()
    return Error.Location(line, column)
  }
}
