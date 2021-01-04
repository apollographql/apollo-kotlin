package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.api.internal.json.Utils.readRecursively
import com.apollographql.apollo.api.internal.json.use
import okio.BufferedSource
import okio.IOException
import kotlin.jvm.JvmStatic

object SimpleOperationResponseParser {

  @JvmStatic
  @Throws(IOException::class)
  fun <D : Operation.Data> parse(
      source: BufferedSource,
      operation: Operation<D, *>,
      customScalarAdapters: CustomScalarAdapters
  ): Response<D> {
    return BufferedSourceJsonReader(source).use { jsonReader ->
      jsonReader.beginObject()

      var data: D? = null
      var errors: List<Error>? = null
      var extensions: Map<String, Any?>? = null
      while (jsonReader.hasNext()) {
        when (jsonReader.nextName()) {
          "data" -> data = jsonReader.readData(
              mapper = operation.responseFieldMapper(),
              variables = operation.variables(),
              customScalarAdapters = customScalarAdapters,
          )
          "errors" -> errors = jsonReader.readErrors()
          "extensions" -> extensions = jsonReader.readExtensions()
          else -> jsonReader.skipValue()
        }
      }

      jsonReader.endObject()

      Response(
          operation = operation,
          data = data,
          errors = errors,
          extensions = extensions.orEmpty()
      )
    }
  }

  private fun <D : Operation.Data> JsonReader.readData(
      mapper: ResponseFieldMapper<D>,
      variables: Operation.Variables,
      customScalarAdapters: CustomScalarAdapters,
  ): D {
    beginObject()
    val data = mapper.map(
        StreamResponseReader(
            jsonReader = this,
            variables = variables,
            customScalarAdapters = customScalarAdapters,
        )
    )
    endObject()
    return data
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

  @Suppress("UNCHECKED_CAST")
  private fun JsonReader.readExtensions(): Map<String, Any?> {
    return readRecursively() as Map<String, Any?>
  }
}
