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
    val resolvedArguments: Any = resolveArguments(field.arguments, variables)
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
  private fun resolveArguments(objectMap: Map<String, Any?>, variables: Operation.Variables): Map<String, Any?> {
    return objectMap.mapValues { (_, value) ->
      if (value is Map<*, *>) {
        val nestedObjectMap = value as Map<String, Any?>
        if (isArgumentValueVariableType(nestedObjectMap)) {
          resolveVariableArgument(nestedObjectMap, variables)
        } else {
          resolveArguments(nestedObjectMap, variables)
        }
      } else {
        value
      }
    }.toList()
        .sortedBy { it.first }
        .toMap()
  }

  @Suppress("UNCHECKED_CAST")
  private fun resolveVariableArgument(objectMap: Map<String, Any?>, variables: Operation.Variables): Any? {
    val variable = objectMap[ResponseField.VARIABLE_NAME_KEY]

    return when (val resolvedVariable = variables.valueMap()[variable]) {
      null -> null
      is Map<*, *> -> resolveArguments(resolvedVariable as Map<String, Any?>, variables)
      is InputType -> {
        val inputFieldMapWriter = SortedInputFieldMapWriter()
        resolvedVariable.marshaller().marshal(inputFieldMapWriter)
        inputFieldMapWriter.map()
      }
      else -> resolvedVariable
    }
  }
}
