package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.ScalarTypeAdapters

object SimpleOperationResponseParser {

  @JvmStatic
  @Suppress("UNCHECKED_CAST")
  fun <D : Operation.Data, W> parse(
      response: Map<String, *>,
      operation: Operation<D, W, *>,
      scalarTypeAdapters: ScalarTypeAdapters
  ): Response<W> {
    val responseData = response["data"] as? Map<String, Any>
    val data = responseData?.let {
      val responseReader = SimpleResponseReader(it, operation.variables(), scalarTypeAdapters)
      operation.responseFieldMapper().map(responseReader)
    }

    val responseErrors = response["errors"] as? List<Map<String, Any>>
    val errors = responseErrors?.let {
      it.map { error -> readError(error) }
    }

    return Response.builder<W>(operation)
        .data(operation.wrapData(data!!))
        .errors(errors)
        .build()
  }

  @Suppress("UNCHECKED_CAST")
  private fun readError(payload: Map<String, Any?>): Error {
    var message: String? = null
    var locations = emptyList<Error.Location>()
    val customAttributes = mutableMapOf<String, Any?>()
    for ((key, value) in payload) {
      when (key) {
        "message" -> message = value?.toString()
        "locations" -> {
          val locationItems = value as? List<Map<String, Any>>
          locations = locationItems?.map { readErrorLocation(it) } ?: emptyList()
        }
        else -> customAttributes[key] = value
      }
    }
    return Error(message, locations, customAttributes)
  }

  private fun readErrorLocation(data: Map<String, Any?>?): Error.Location {
    var line: Long = -1
    var column: Long = -1
    if (data != null) {
      for ((key, value) in data) {
        when (key) {
          "line" -> line = (value as Number).toLong()
          "column" -> column = (value as Number).toLong()
        }
      }
    }
    return Error.Location(line, column)
  }
}
