package com.apollographql.apollo.response

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.Response.Companion.builder
import com.apollographql.apollo.api.internal.FieldValueResolver
import com.apollographql.apollo.api.internal.RealResponseReader
import com.apollographql.apollo.api.internal.ResolveDelegate
import com.apollographql.apollo.api.internal.Utils.__checkNotNull
import com.apollographql.apollo.api.internal.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.internal.json.Utils.readRecursively
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer
import com.apollographql.apollo.internal.field.MapFieldValueResolver
import okio.BufferedSource
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

class OperationResponseParser<D : Operation.Data> @JvmOverloads constructor(
    val operation: Operation<D>,
    val customScalarAdapters: CustomScalarAdapters,
    val responseNormalizer: ResponseNormalizer<Map<String, Any?>?> = ResponseNormalizer.NO_OP_NORMALIZER as ResponseNormalizer<Map<String, Any?>?>) {
  fun parse(payload: Map<String, Any?>): Response<D> {
    responseNormalizer.willResolveRootQuery(operation)
    var data: D? = null
    val buffer = payload["data"] as Map<String, Any?>?
    if (buffer != null) {
      val realResponseReader = RealResponseReader(
          operation.variables(),
          buffer,
          MapFieldValueResolver() as FieldValueResolver<Map<String, Any?>>,
          customScalarAdapters,
          responseNormalizer as ResolveDelegate<Map<String, Any?>>
      )
      data = operation.adapter().fromResponse(realResponseReader, null)
    }
    var errors: MutableList<Error>? = null
    if (payload.containsKey("errors")) {
      val errorPayloads = payload["errors"] as List<Map<String, Any?>>?
      if (errorPayloads != null) {
        errors = ArrayList()
        for (errorPayload in errorPayloads) {
          errors.add(parseError(errorPayload))
        }
      }
    }
    return builder<D>(operation)
        .data(data)
        .errors(errors)
        .dependentKeys(responseNormalizer.dependentKeys())
        .extensions(payload["extensions"] as Map<String, Any?>?)
        .build()
  }

  @Throws(IOException::class)
  fun parse(source: BufferedSource?): Response<D> {
    var jsonReader: BufferedSourceJsonReader? = null
    return try {
      jsonReader = BufferedSourceJsonReader(source!!)
      parse((jsonReader.readRecursively() as Map<String, Any?>?)!!)
    } finally {
      jsonReader?.close()
    }
  }

  companion object {
    fun parseError(payload: Map<String, Any?>): Error {
      var message = ""
      val locations: MutableList<Error.Location> = ArrayList()
      val customAttributes: MutableMap<String, Any?> = HashMap()
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
}