package com.apollographql.apollo.api.internal

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.DeferredFragmentIdentifier
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.falseVariables
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.MapJsonReader
import com.apollographql.apollo.api.json.readAny
import com.apollographql.apollo.api.parseData
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

/**
 * [ResponseParser] parses network responses, including data, errors and extensions from a [JsonReader]
 */
internal object ResponseParser {
  fun <D : Operation.Data> parse(
      jsonReader: JsonReader,
      operation: Operation<D>,
      requestUuid: Uuid?,
      customScalarAdapters: CustomScalarAdapters,
      deferredFragmentIds: Set<DeferredFragmentIdentifier>?,
  ): ApolloResponse<D> {
    jsonReader.beginObject()

    var data: D? = null
    var errors: List<Error>? = null
    var extensions: Map<String, Any?>? = null
    while (jsonReader.hasNext()) {
      @Suppress("UNCHECKED_CAST")
      when (jsonReader.nextName()) {
        "data" -> {
          val falseVariables = operation.falseVariables(customScalarAdapters)
          data = operation.parseData(jsonReader, customScalarAdapters, falseVariables, deferredFragmentIds, errors)
        }
        "errors" -> errors = jsonReader.readErrors()
        "extensions" -> extensions = jsonReader.readAny() as? Map<String, Any?>
        else -> jsonReader.skipValue()
      }
    }

    jsonReader.endObject()

    return ApolloResponse.Builder(operation = operation, requestUuid = requestUuid ?: uuid4())
        .errors(errors)
        .data(data)
        .extensions(extensions)
        .build()
  }

  fun parseError(
      payload: Map<String, Any?>,
  ) = MapJsonReader(payload).readError()
}

@Suppress("UNCHECKED_CAST")
private fun JsonReader.readError(): Error {
  var message = ""
  var locations: List<Error.Location>? = null
  var path: List<Any>? = null
  var extensions: Map<String, Any?>? = null
  var nonStandardFields: MutableMap<String, Any?>? = null
  beginObject()
  while (hasNext()) {
    when (val name = nextName()) {
      "message" -> message = nextString() ?: ""
      "locations" -> {
        locations = readErrorLocations()
      }

      "path" -> {
        path = readPath()
      }

      "extensions" -> {
        extensions = readAny() as? Map<String, Any?>?
      }

      else -> {
        if (nonStandardFields == null) nonStandardFields = mutableMapOf()
        nonStandardFields[name] = readAny()
      }
    }
  }
  endObject()


  @Suppress("DEPRECATION")
  return Error(message, locations, path, extensions, nonStandardFields)
}

private fun JsonReader.readPath(): List<Any>? {
  if (peek() == JsonReader.Token.NULL) {
    return nextNull()
  }

  val list = mutableListOf<Any>()
  beginArray()
  while (hasNext()) {
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
  while (hasNext()) {
    list.add(readErrorLocation())
  }
  endArray()

  return list
}

private fun JsonReader.readErrorLocation(): Error.Location {
  var line: Int = -1
  var column: Int = -1
  beginObject()
  while (hasNext()) {
    when (nextName()) {
      "line" -> line = nextInt()
      "column" -> column = nextInt()
      else -> skipValue()
    }
  }
  endObject()
  return Error.Location(line, column)
}

@ApolloInternal
fun JsonReader.readErrors(): List<Error> {
  if (peek() == JsonReader.Token.NULL) {
    nextNull()
    return emptyList()
  }

  beginArray()
  val list = mutableListOf<Error>()
  while (hasNext()) {
    list.add(readError())
  }
  endArray()
  return list
}