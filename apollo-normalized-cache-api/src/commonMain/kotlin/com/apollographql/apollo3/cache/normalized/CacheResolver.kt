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

  /**
   * Returns a cache key for the given object
   *
   * @param type the type of the object
   * @param obj the object
   */
  open fun cacheKeyForObject(
      type: CompiledNamedType,
      obj: Map<String, @JvmSuppressWildcards Any?>,
  ): CacheKey? {
    val keyFields = type.keyFields()

    if (keyFields.isNotEmpty()) {
      return buildCacheKey(type.name, keyFields.map { obj[it].toString() })
    }

    return null
  }

  /**
   * Resolves a field from the cache. This API is similar to a backend side resolver in that it allows resolving fields to arbitrary
   * values.
   *
   * @param field the field to resolve
   * @param variables the variables of the current operation
   * @param parent the parent object as a map. It can contain the same values as [Record]. Especially, nested objects will be represented
   * by [CacheKey]
   * @param parentId the id of the parent. Mainly used for debugging
   */
  open fun resolveField(
      field: CompiledField,
      variables: Executable.Variables,
      parent: Map<String, @JvmSuppressWildcards Any?>,
      parentId: String,
  ): Any? {
    val keyArgsValues = field.arguments.filter { it.isKey }.map {
      CompiledArgument.resolveVariables(it.value, variables).toString()
    }

    if (keyArgsValues.isNotEmpty()) {
      return buildCacheKey(field.type.leafType().name, keyArgsValues)
    }

    val name = field.nameWithArguments(variables)
    if (!parent.containsKey(name)) {
      throw CacheMissException(parentId, name)
    }

    return parent[name]
  }

  /**
   * Helper function to build a cache key from a list of strings
   */
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
}

class IdCacheResolver: CacheResolver() {
  override fun cacheKeyForObject(type: CompiledNamedType, obj: Map<String, Any?>): CacheKey? {
    return obj["id"]?.toString()?.let { CacheKey(it) }
  }

  override fun resolveField(field: CompiledField, variables: Executable.Variables, parent: Map<String, Any?>, parentId: String): Any? {
    val id = field.resolveArgument("id", variables)?.toString()
    if (id != null) {
       return CacheKey(id)
    }

    val name = field.nameWithArguments(variables)
    if (!parent.containsKey(name)) {
      throw CacheMissException(parentId, name)
    }

    return parent[name]
  }
}
