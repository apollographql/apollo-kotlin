package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.api.CompiledArgument
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.InterfaceType
import com.apollographql.apollo3.api.ObjectType
import com.apollographql.apollo3.api.UnionType
import com.apollographql.apollo3.api.exception.CacheMissException
import com.apollographql.apollo3.api.isCompound
import com.apollographql.apollo3.api.keyFields
import com.apollographql.apollo3.api.leafType
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSuppressWildcards

open class CacheResolver {

  open fun cacheKeyForObject(
      field: CompiledField,
      variables: Executable.Variables,
      map: Map<String, @JvmSuppressWildcards Any?>,
  ): CacheKey? {
    val keyFields = field.type.leafType().keyFields()

    if (keyFields.isNotEmpty()) {
      return buildCacheKey(field, keyFields.map { map[it].toString() })
    }

    return null
  }

  protected fun buildCacheKey(field: CompiledField, values: List<String>): CacheKey {
    val typeName = field.type.leafType().name
    return CacheKey(
        buildString {
          append(typeName)
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
      return buildCacheKey(field, keyArgsValues)
    }

    val name = field.nameWithArguments(variables)
    if (!parent.containsKey(name)) {
      throw CacheMissException(parentKey, name)
    }

    return parent[name]
  }
}

class IdCacheResolver: CacheResolver() {
  override fun cacheKeyForObject(field: CompiledField, variables: Executable.Variables, map: Map<String, Any?>): CacheKey? {
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