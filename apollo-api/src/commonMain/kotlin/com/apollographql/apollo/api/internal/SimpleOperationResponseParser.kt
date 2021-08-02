package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.ResponseJsonStreamReader
import com.apollographql.apollo.api.internal.json.use
import okio.BufferedSource
import okio.IOException
import kotlin.jvm.JvmStatic

object SimpleOperationResponseParser {

  @JvmStatic
  @Throws(IOException::class)
  fun <D : Operation.Data, W> parse(
      source: BufferedSource,
      operation: Operation<D, W, *>,
      scalarTypeAdapters: ScalarTypeAdapters
  ): Response<W> {
    return BufferedSourceJsonReader(source).use { jsonReader ->
      jsonReader.beginObject()
      val response = ResponseJsonStreamReader(jsonReader).toMap().orEmpty()
      parse(response, operation, scalarTypeAdapters)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <D : Operation.Data, W> parse(
      response: Map<String, Any?>,
      operation: Operation<D, W, *>,
      scalarTypeAdapters: ScalarTypeAdapters
  ): Response<W> {
    val responseData = response["data"] as? Map<String, Any?>
    val data = responseData?.let {
      val responseReader = SimpleResponseReader(it, operation.variables(), scalarTypeAdapters)
      operation.responseFieldMapper().map(responseReader)
    }

    val responseErrors = response["errors"] as? List<Map<String, Any?>>
    val errors = responseErrors?.let {
      it.map { errorPayload -> errorPayload.readError() }
    }

    return Response(
        operation = operation,
        data = operation.wrapData(data),
        errors = errors,
        extensions = (response["extensions"] as? Map<String, Any?>?).orEmpty()
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun Map<String, Any?>.readError(): Error {
    var message = ""
    var locations = emptyList<Error.Location>()
    var path: List<Any>? = null
    val customAttributes = mutableMapOf<String, Any?>()
    for ((key, value) in this) {
      when (key) {
        "message" -> message = value?.toString() ?: ""
        "locations" -> {
          val locationItems = value as? List<Map<String, Any?>>
          locations = locationItems?.map { it.readErrorLocation() } ?: emptyList()
        }
        "path" -> {
          path = (value as? List<Any>)?.map { if (it is Number) it.toInt() else it }
        }
        else -> customAttributes[key] = value
      }
    }
    return Error(message, locations, path, customAttributes)
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
