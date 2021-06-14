package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.api.CompiledCompoundType
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.exception.CacheMissException
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSuppressWildcards

open class CacheResolver {
  protected fun cacheKey(map: Map<String, Any?>, vararg names: String): CacheKey {
    return CacheKey(
        buildString {
          append(map["__typename"])
          names.forEach {
            append(map[it])
          }
        }
    )
  }

  open fun cacheKeyForObject(
      field: CompiledField,
      variables: Executable.Variables,
      map: Map<String, @JvmSuppressWildcards Any?>,
  ): CacheKey? {
    return null
  }

  open fun resolveField(
      field: CompiledField,
      variables: Executable.Variables,
      parent: Map<String, @JvmSuppressWildcards Any?>,
      parentKey: String,
  ): Any? {
    val name = field.nameWithArguments(variables)
    if (!parent.containsKey(name)) {
      throw CacheMissException(parentKey, name)
    }

    return parent[name]
  }

  companion object {
    private val ROOT_CACHE_KEY = CacheKey("QUERY_ROOT")

    @JvmField
    val DEFAULT = CacheResolver()

    val ID = object : CacheResolver() {
      override fun cacheKeyForObject(field: CompiledField, variables: Executable.Variables, map: Map<String, Any?>): CacheKey? {
        return map["id"]?.toString()?.let { CacheKey(it) }
      }

      override fun resolveField(field: CompiledField, variables: Executable.Variables, parent: Map<String, Any?>, parentKey: String): Any? {
        if (field.type !is CompiledCompoundType) {
          // scalar fields cannot be resolved to a CacheKey
          return super.resolveField(field, variables, parent, parentKey)
        }

        val id = field.resolveArgument("id", variables)?.toString()
        return if (id != null) {
          CacheKey(id)
        } else {
          super.resolveField(field, variables, parent, parentKey)
        }
      }
    }

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun rootKey(): CacheKey {
      return ROOT_CACHE_KEY
    }
  }
}
