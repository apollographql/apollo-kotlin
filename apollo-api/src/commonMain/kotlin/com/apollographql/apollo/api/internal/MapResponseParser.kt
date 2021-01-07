package com.apollographql.apollo.api.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.Response.Companion.builder
import com.apollographql.apollo.api.ResponseField

object MapResponseParser {
  class MapFieldValueResolver : FieldValueResolver<Map<String, Any>> {
    override fun <T> valueFor(map: Map<String, Any>, field: ResponseField): T? {
      return map[field.responseName] as T?
    }
  }

  fun <D : Operation.Data> parse(
      payload: Map<String, Any?>,
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters
  ): Response<D> {
    var data: D? = null
    val buffer = payload["data"] as Map<String, Any?>?
    if (buffer != null) {
      val realResponseReader = RealResponseReader(
          operation.variables(),
          buffer,
          MapFieldValueResolver() as FieldValueResolver<Map<String, Any?>>,
          customScalarAdapters,
      )
      data = operation.adapter().fromResponse(realResponseReader, null)
    }
    var errors: MutableList<Error>? = null
    if (payload.containsKey("errors")) {
      val errorPayloads = payload["errors"] as List<Map<String, Any?>>?
      if (errorPayloads != null) {
        errors = mutableListOf()
        for (errorPayload in errorPayloads) {
          errors.add(parseError(errorPayload))
        }
      }
    }
    return builder<D>(operation)
        .data(data)
        .errors(errors)
        .extensions(payload["extensions"] as Map<String, Any?>?)
        .build()
  }

  fun parseError(payload: Map<String, Any?>): Error {
    var message = ""
    val locations = mutableListOf<Error.Location>()
    val customAttributes = mutableMapOf<String, Any?>()
    for ((key, value) in payload) {
      if ("message" == key) {
        message = value?.toString() ?: ""
      } else if ("locations" == key) {
        val locationItems = value as List<Map<String, Any>>?
        if (locationItems != null) {
          for (item in locationItems) {
            locations.add(parseErrorLocation(item))
          }
        }
      } else {
        if (value != null) {
          customAttributes[key] = value
        }
      }
    }
    return Error(message, locations, customAttributes)
  }

  private fun parseErrorLocation(data: Map<String, Any>?): Error.Location {
    var line: Long = -1
    var column: Long = -1
    if (data != null) {
      for ((key, value) in data) {
        if ("line" == key) {
          line = (value as Number).toLong()
        } else if ("column" == key) {
          column = (value as Number).toLong()
        }
      }
    }
    return Error.Location(line, column)
  }
}