package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.Variable
import com.apollographql.apollo3.api.internal.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.internal.json.Utils
import okio.Buffer
import okio.IOException

class RealCacheKeyBuilder : CacheKeyBuilder {

  override fun build(field: CompiledField, variables: Executable.Variables): String {
    if (field.arguments.isEmpty()) {
      return field.name
    }
    val resolvedArguments = resolveVariables(field.arguments, variables)
    return try {
      val buffer = Buffer()
      val jsonWriter = BufferedSinkJsonWriter(buffer)
      Utils.writeToJson(resolvedArguments, jsonWriter)
      jsonWriter.close()
      "${field.name}(${buffer.readUtf8()})"
    } catch (e: IOException) {
      throw RuntimeException(e)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun resolveVariables(value: Any?, variables: Executable.Variables): Any? {
    return when (value) {
      null -> null
      is Variable -> {
        variables.valueMap[value.name]
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
}
