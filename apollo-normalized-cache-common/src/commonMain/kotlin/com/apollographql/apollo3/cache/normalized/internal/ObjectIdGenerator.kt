package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CompiledNamedType
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.keyFields
import com.apollographql.apollo3.cache.normalized.CacheKey

/**
 * An [ObjectIdGenerator] is responsible for finding an id for a given object
 * Called during normalization after a network reponse has been received to build
 * the [Record] that will be stored in cache
 *
 * See also `@typePolicy`
 */
interface ObjectIdGenerator {
  /**
   * Returns a [CacheKey] for the given object or null if the object doesn't have an id
   *
   * @param type the concrete type of the object. Use this with [CacheKey.from] to namespace the ids
   * @param obj a [Map] representing the object. The values in the map can have the same types as the ones
   * in [Record]
   * @param context the context in which the object is. In most use cases, the id should not depend on the context.
   * Only use for advanced use cases.
   */
  fun generateIdFor(type: CompiledNamedType, obj: Map<String, Any?>, context: ObjectIdGeneratorContext): CacheKey?
}

/**
 * The context in which an object is normalized.
 *
 * @param field the field representing the object or for lists, the field representing the list. [field.type] is not
 * always the type of the object. Especially, it can be any combination of [CompiledNotNullType] and [CompiledListType]
 * @param variables the variables used in the operation where the object is normalized.
 */
class ObjectIdGeneratorContext(
    field: CompiledField,
    variables: Executable.Variables,
)

/**
 * A [ObjectIdGenerator] that always uses the "id" field if it exists and delegates to [TypePolicyObjectIdGenerator] else
 *
 * It will coerce Int, Floats and other types to String using [toString]
 */
object IdObjectIdGenerator : ObjectIdGenerator {
  override fun generateIdFor(type: CompiledNamedType, obj: Map<String, Any?>, context: ObjectIdGeneratorContext): CacheKey? {
    return obj["id"]?.toString()?.let { CacheKey(it) } ?: TypePolicyObjectIdGenerator.generateIdFor(type, obj, context)
  }
}

/**
 * A [ObjectIdGenerator] that uses annotations to compute the id
 */
object TypePolicyObjectIdGenerator : ObjectIdGenerator {
  override fun generateIdFor(type: CompiledNamedType, obj: Map<String, Any?>, context: ObjectIdGeneratorContext): CacheKey? {
    val keyFields = type.keyFields()

    return if (keyFields.isNotEmpty()) {
      CacheKey.from(type.name, keyFields.map { obj[it].toString() })
    } else {
      null
    }
  }
}