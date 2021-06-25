package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.api.CompiledArgument
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.api.keyFields
import com.apollographql.apollo3.api.leafType
import kotlin.jvm.JvmSuppressWildcards

open class CacheResolver {

  open fun cacheKeyForObject(
      type: CompiledNamedType,
      variables: Executable.Variables,
      map: Map<String, @JvmSuppressWildcards Any?>,
  ): CacheKey? {
    val keyFields = type.keyFields()

    if (keyFields.isNotEmpty()) {
      return buildCacheKey(type.name, keyFields.map { map[it].toString() })
    }

    return null
  }

  protected fun buildCacheKey(typename: String, values: List<String>): CacheKey {
    return CacheKey(
        buildString {
          append(typename)
          append(":")
          values.forEach {
            append(it)
          }
        }
    )
  }

  open fun resolveField(
      field: CompiledField,
      variables: Executable.Variables,
      parent: Map<String, @JvmSuppressWildcards Any?>,
      parentKey: String,
  ): Any? {
    val keyArgsValues = field.arguments.filter { it.isKey }.map {
      CompiledArgument.resolveVariables(it.value, variables).toString()
    }

    if (keyArgsValues.isNotEmpty()) {
      return buildCacheKey(field.type.leafType().name, keyArgsValues)
    }

    val name = field.nameWithArguments(variables)
    if (!parent.containsKey(name)) {
      throw CacheMissException(parentKey, name)
    }

    return parent[name]
  }
}

class IdCacheResolver: CacheResolver() {
  override fun cacheKeyForObject(type: CompiledNamedType, variables: Executable.Variables, map: Map<String, Any?>): CacheKey? {
    return map["id"]?.toString()?.let { CacheKey(it) }
  }

  override fun resolveField(field: CompiledField, variables: Executable.Variables, parent: Map<String, Any?>, parentKey: String): Any? {
    val id = field.resolveArgument("id", variables)?.toString()
    if (id != null) {
       return CacheKey(id)
    }

    val name = field.nameWithArguments(variables)
    if (!parent.containsKey(name)) {
      throw CacheMissException(parentKey, name)
    }

    return parent[name]
  }
}
