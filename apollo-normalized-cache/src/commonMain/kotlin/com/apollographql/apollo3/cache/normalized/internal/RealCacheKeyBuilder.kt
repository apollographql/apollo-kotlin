package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.InputType
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.api.Variable
import com.apollographql.apollo3.api.internal.json.JsonWriter
import com.apollographql.apollo3.api.internal.json.Utils
import okio.Buffer
import okio.IOException

class RealCacheKeyBuilder : CacheKeyBuilder {

  override fun build(field: ResponseField, variables: Operation.Variables): String {
    if (field.arguments.isEmpty()) {
      return field.fieldName
    }
    val resolvedArguments = resolveVariables(field.arguments, variables)
    return try {
      val buffer = Buffer()
      val jsonWriter = JsonWriter.of(buffer)
      jsonWriter.serializeNulls = true
      Utils.writeToJson(resolvedArguments, jsonWriter)
      jsonWriter.close()
      "${field.fieldName}(${buffer.readUtf8()})"
    } catch (e: IOException) {
      throw RuntimeException(e)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun resolveVariables(value: Any?, variables: Operation.Variables): Any? {
    return when (value) {
      null -> null
      is Variable -> {
        resolveVariable(value.name, variables)
      }
      is Map<*, *> -> {
        value as Map<String, Any?>
        value.mapValues {
          resolveVariables(it.value, variables)
        }.toList()
            .sortedBy { it.first }
            .toMap()
      }
      is List<*> -> {
        value.map {
          resolveVariables(it, variables)
        }
      }
      else -> value
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun resolveVariable(name: String, variables: Operation.Variables): Any? {
    return when (val resolvedVariable = variables.valueMap()[name]) {
      is InputType -> {
        val inputFieldMapWriter = SortedInputFieldMapWriter()
        resolvedVariable.marshaller().marshal(inputFieldMapWriter)
        inputFieldMapWriter.map()
      }
      else -> resolvedVariable
    }
  }
}
