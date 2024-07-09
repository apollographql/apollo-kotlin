package com.apollographql.apollo.cache.normalized.api

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.CompiledField
import com.apollographql.apollo.api.Executable
import com.apollographql.apollo.exception.CacheMissException
import com.apollographql.apollo.mpp.currentTimeMillis
import kotlin.jvm.JvmSuppressWildcards

/**
 * An interface for [CacheResolver] used to read the cache
 */
interface CacheResolver {
  /**
   * Resolves a field from the cache. Called when reading from the cache, usually before a network request.
   * - takes a GraphQL field and operation variables as input and generates data for this field
   * - this data can be a CacheKey for objects but it can also be any other data if needed. In that respect,
   * it's closer to a resolver as might be found in apollo-server
   * - is used before a network request
   * - is used when reading the cache
   *
   * It can be used to map field arguments to [CacheKey]:
   *
   * ```
   * {
   *   user(id: "1"}) {
   *     id
   *     firstName
   *     lastName
   *   }
   * }
   * ```
   *
   * ```
   * override fun resolveField(field: CompiledField, variables: Executable.Variables, parent: Map<String, Any?>, parentId: String): Any? {
   *   val id = field.resolveArgument("id", variables)?.toString()
   *   if (id != null) {
   *     return CacheKey(id)
   *   }
   *
   *   return super.resolveField(field, variables, parent, parentId)
   * }
   * ```
   *
   * The simple example above isn't very representative as most of the times `@fieldPolicy` can express simple argument mappings in a more
   * concise way but still demonstrates how [resolveField] works.
   *
   * [resolveField] can also be generalized to return any value:
   *
   * ```
   * override fun resolveField(field: CompiledField, variables: Executable.Variables, parent: Map<String, Any?>, parentId: String): Any? {
   *   if (field.name == "name") {
   *     // Every "name" field will return "JohnDoe" now!
   *     return "JohnDoe"
   *   }
   *
   *   return super.resolveField(field, variables, parent, parentId)
   * }
   * ```
   *
   * See also @fieldPolicy
   * See also [CacheKeyGenerator]
   *
   * @param field the field to resolve
   * @param variables the variables of the current operation
   * @param parent the parent object as a map. It can contain the same values as [Record]. Especially, nested objects will be represented
   * by [CacheKey]
   * @param parentId the id of the parent. Mainly used for debugging
   *
   * @return a value that can go in a [Record]. No type checking is done. It is the responsibility of implementations to return the correct
   * type
   */
  fun resolveField(
      field: CompiledField,
      variables: Executable.Variables,
      parent: Map<String, @JvmSuppressWildcards Any?>,
      parentId: String,
  ): Any?
}

/**
 * A cache resolver that uses the parent to resolve fields.
 */
object DefaultCacheResolver : CacheResolver {
  /**
   * @param parent a [Map] that represent the object containing this field. The map values can have the same types as the ones in  [Record]
   */
  override fun resolveField(
      field: CompiledField,
      variables: Executable.Variables,
      parent: Map<String, @JvmSuppressWildcards Any?>,
      parentId: String,
  ): Any? {
    val fieldKey = field.nameWithArguments(variables)
    if (!parent.containsKey(fieldKey)) {
      throw CacheMissException(parentId, fieldKey)
    }

    return parent[fieldKey]
  }
}


/**
 * A cache resolver that uses the cache date as a receive date and expires after a fixed max age
 */
@ApolloExperimental
class ReceiveDateCacheResolver(private val maxAge: Int) : CacheResolver {
  /**
   * @param parent a [Map] that represent the object containing this field. The map values can have the same types as the ones in  [Record]
   */
  override fun resolveField(
      field: CompiledField,
      variables: Executable.Variables,
      parent: Map<String, @JvmSuppressWildcards Any?>,
      parentId: String,
  ): Any? {
    val fieldKey = field.nameWithArguments(variables)
    if (!parent.containsKey(fieldKey)) {
      throw CacheMissException(parentId, fieldKey)
    }

    if (parent is Record) {
      val lastUpdated = parent.date?.get(fieldKey)
      if (lastUpdated != null) {
        val age = currentTimeMillis() / 1000 - lastUpdated
        if (age > maxAge) {
          throw CacheMissException(parentId, fieldKey, true)
        }
      }
    }

    return parent[fieldKey]
  }
}

/**
 * A cache resolver that uses the cache date as an expiration date and expires past it
 */
@ApolloExperimental
class ExpireDateCacheResolver() : CacheResolver {
  /**
   * @param parent a [Map] that represent the object containing this field. The map values can have the same types as the ones in  [Record]
   */
  override fun resolveField(
      field: CompiledField,
      variables: Executable.Variables,
      parent: Map<String, @JvmSuppressWildcards Any?>,
      parentId: String,
  ): Any? {
    val fieldKey = field.nameWithArguments(variables)
    if (!parent.containsKey(fieldKey)) {
      throw CacheMissException(parentId, fieldKey)
    }

    if (parent is Record) {
      val expires = parent.date?.get(fieldKey)
      if (expires != null) {
        if (currentTimeMillis() / 1000 - expires >= 0) {
          throw CacheMissException(parentId, fieldKey, true)
        }
      }
    }

    return parent[fieldKey]
  }
}

/**
 * A [CacheResolver] that uses @fieldPolicy annotations to resolve fields and delegates to [DefaultCacheResolver] else
 */
object FieldPolicyCacheResolver : CacheResolver {
  override fun resolveField(
      field: CompiledField,
      variables: Executable.Variables,
      parent: Map<String, @JvmSuppressWildcards Any?>,
      parentId: String,
  ): Any? {
    val keyArgsValues = field.argumentValues(variables) { it.definition.isKey }.values.map { it.toString() }

    if (keyArgsValues.isNotEmpty()) {
      return CacheKey(field.type.rawType().name, keyArgsValues)
    }

    return DefaultCacheResolver.resolveField(field, variables, parent, parentId)
  }
}


