package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.api.CompiledArgument
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.leafType
import com.apollographql.apollo3.exception.CacheMissException
import kotlin.jvm.JvmSuppressWildcards

/**
 * An interface for [CacheResolver] used to read the cache
 */
interface CacheResolver {
  /**
   * Resolves a field from the cache. Called when reading from the cache, usually before a network request.
   * This API is similar to a backend side resolver in that it allows resolving fields to arbitrary values.
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
 * A cache resolver that uses the parent to resolve fields. [parent] is a [Map] that
 * can contain the same values as [Record]
 */
object MapCacheResolver: CacheResolver {
  override fun resolveField(
      field: CompiledField,
      variables: Executable.Variables,
      parent: Map<String, @JvmSuppressWildcards Any?>,
      parentId: String,
  ): Any? {
    val name = field.nameWithArguments(variables)
    if (!parent.containsKey(name)) {
      throw CacheMissException(parentId, name)
    }

    return parent[name]
  }
}

/**
 * A [CacheResolver] that uses @fieldPolicy annotations to resolve fields and delegates to [MapCacheResolver] else
 */
object FieldPolicyCacheResolver: CacheResolver {
  override fun resolveField(
      field: CompiledField,
      variables: Executable.Variables,
      parent: Map<String, @JvmSuppressWildcards Any?>,
      parentId: String,
  ): Any? {
    val keyArgsValues = field.arguments.filter { it.isKey }.map {
      CompiledArgument.resolveVariables(it.value, variables).toString()
    }

    if (keyArgsValues.isNotEmpty()) {
      return CacheKey.from(field.type.leafType().name, keyArgsValues)
    }

    return MapCacheResolver.resolveField(field, variables, parent, parentId)
  }
}


