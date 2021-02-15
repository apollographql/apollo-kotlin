package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.InputType
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseField.Companion.isArgumentValueVariableType
import com.apollographql.apollo.api.internal.json.JsonWriter
import com.apollographql.apollo.api.internal.json.Utils
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
      is Map<*, *> -> {
        value as Map<String, Any?>
        if (isArgumentValueVariableType(value)) {
          resolveVariable(value, variables)
        } else {
          value.mapValues {
            resolveVariables(it.value, variables)
          }.toList()
              .sortedBy { it.first }
              .toMap()
        }
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
  private fun resolveVariable(objectMap: Map<String, Any?>, variables: Operation.Variables): Any? {
    val variable = objectMap[ResponseField.VARIABLE_NAME_KEY]

    return when (val resolvedVariable = variables.valueMap()[variable]) {
      is InputType -> {
        val inputFieldMapWriter = SortedInputFieldMapWriter()
        resolvedVariable.marshaller().marshal(inputFieldMapWriter)
        inputFieldMapWriter.map()
      }
      else -> resolvedVariable
    }
  }
}
