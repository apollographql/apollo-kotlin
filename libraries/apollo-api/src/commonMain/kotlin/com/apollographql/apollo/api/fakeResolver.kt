package com.apollographql.apollo.api

import com.apollographql.apollo.api.json.MapJsonReader
import com.apollographql.apollo.api.json.MapJsonWriter
import kotlin.error
import kotlin.math.absoluteValue

/**
 * @property path the path to the element being resolved. [path] is a list of [String] or [Int]
 * @property id the id of the field. By default, it is the path of the field or the cache key
 * if `@typePolicy` is used. You can override this with [FakeResolver.stableIdForObject]
 * @property mergedField the field being resolved
 */
class FakeResolverContext internal constructor(
    val path: List<Any>,
    val id: String,
    val mergedField: CompiledField,
    val customScalarAdapters: CustomScalarAdapters
)

/**
 * Provides fakes values for Data builders
 */
interface FakeResolver {
  /**
   * Resolves a leaf (scalar or enum) type. Note that because of list and not-nullable
   * types, the type of `context.mergedField` is not always the leaf type.
   * You can get the type of the leaf type with:
   *
   * ```
   * context.mergedField.type.rawType()
   * ```
   *
   * @return a kotlin value representing the value at path `context.path`. Possible values include
   * - Boolean
   * - Int
   * - Double
   * - Strings
   * - Custom scalar targets
   */
  fun resolveLeaf(context: FakeResolverContext): Any

  /**
   * Resolves the size of a list. Note that lists might be nested. You can use `context.path` to get
   * the current nesting depth
   */
  fun resolveListSize(context: FakeResolverContext): Int

  /**
   * @return true if the current value should return null
   */
  fun resolveMaybeNull(context: FakeResolverContext): Boolean

  /**
   * @return a concrete type that implements the type in `context.mergedField.type.rawType()`
   */
  fun resolveTypename(context: FakeResolverContext): String

  /**
   * Use [stableIdForObject] to provide [FakeResolverContext.id]. You can then use [FakeResolverContext.id]
   * to derive stable values in the resolveXyz() methods above. This way, you're guaranteed that a fake object will have
   * the same field values no matter its path in the query.
   * @param obj the representation of the object. It contains all user defined values. If you do not provide a value, this
   * method is not called.
   * @return a cache key for the given field
   */
  fun stableIdForObject(obj: Map<String, Any?>, mergedField: CompiledField): String?
}

private fun collect(selections: List<CompiledSelection>, typename: String): List<CompiledField> {
  return selections.flatMap { compiledSelection ->
    when (compiledSelection) {
      is CompiledField -> {
        listOf(compiledSelection)
      }

      is CompiledFragment -> {
        if (typename in compiledSelection.possibleTypes) {
          collect(compiledSelection.selections, typename)
        } else {
          emptyList()
        }
      }
    }
  }
}


private fun collectAndMerge(selections: List<CompiledSelection>, typename: String): List<CompiledField> {
  /**
   * This doesn't check the condition and will therefore overfetch
   */
  return collect(selections, typename).groupBy { it.responseName }.values.map { fields ->
    val first = fields.first()

    CompiledField.Builder(first.name, first.type)
        .alias(first.alias)
        .selections(fields.flatMap { field -> field.selections })
        .build()
  }
}

/**
 * @param selections the selections of the operation to fake
 * @param typename the type of the object currently being resolved. Always an object type
 */
private fun buildFakeObject(
    selections: List<CompiledSelection>,
    typename: String,
    base: Map<String, Any?>,
    resolver: FakeResolver,
    customScalarAdapters: CustomScalarAdapters,
): Map<String, Any?> {
  @Suppress("UNCHECKED_CAST")
  return buildFieldOfType(
      emptyList(),
      "",
      CompiledField.Builder("data", CompiledNotNullType(ObjectType.Builder(typename).build()))
          .selections(selections)
          .build(),
      resolver,
      Optional.Present(base),
      CompiledNotNullType(ObjectType.Builder(typename).build()),
      customScalarAdapters
  ) as Map<String, Any?>
}

private fun Map<String, Any?>.getOrAbsent(key: String) = if (containsKey(key)) {
  Optional.Present(get(key))
} else {
  Optional.Absent
}

private fun buildFieldOfType(
    path: List<Any>,
    id: String,
    mergedField: CompiledField,
    resolver: FakeResolver,
    value: Optional<Any?>,
    type: CompiledType,
    customScalarAdapters: CustomScalarAdapters,
): Any? {
  if (value is Optional.Present && value.value is Optional.Absent) {
    // Shortcut to allow omitting a value for `@skip` directives
    return Optional.Absent
  }
  if (type !is CompiledNotNullType) {
    return if (value is Optional.Present) {
      if (value.value == null) {
        null
      } else {
        buildFieldOfType(path, id, mergedField, resolver, value, CompiledNotNullType(type), customScalarAdapters)
      }
    } else {
      if (resolver.resolveMaybeNull(FakeResolverContext(path, id, mergedField, customScalarAdapters))) {
        null
      } else {
        buildFieldOfType(path, id, mergedField, resolver, value, CompiledNotNullType(type), customScalarAdapters)
      }
    }
  }

  return buildFieldOfNonNullType(path, id, mergedField, resolver, value, type.ofType, customScalarAdapters)
}

private fun buildFieldOfNonNullType(
    path: List<Any>,
    id: String,
    mergedField: CompiledField,
    resolver: FakeResolver,
    value: Optional<Any?>,
    type: CompiledType,
    customScalarAdapters: CustomScalarAdapters,
): Any? {
  return when (type) {
    is CompiledListType -> {
      if (value is Optional.Present) {
        val list = (value.value as? List<Any?>) ?: error("")
        list.mapIndexed { index, item ->
          buildFieldOfType(path + index, id, mergedField, resolver, Optional.Present(item), type.ofType, customScalarAdapters)
        }
      } else {
        0.until(resolver.resolveListSize(FakeResolverContext(path, id, mergedField, customScalarAdapters))).map {
          buildFieldOfType(path + it, id + it, mergedField, resolver, Optional.Absent, type.ofType, customScalarAdapters)
        }
      }
    }

    is CompiledNamedType -> {
      if (value is Optional.Present) {
        if (mergedField.selections.isNotEmpty()) {
          @Suppress("UNCHECKED_CAST")
          val map = (value.value as? Map<String, Any?>) ?: error("")

          /**
           * If the map was created through one of the builders, we are guaranteed that __typename
           * is present because it was created as a concrete type.
           * It might still be empty if users create fragments manually
           */
          val typename = (map["__typename"] as? String) ?: "__unknown"

          val stableId = resolver.stableIdForObject(map, mergedField) ?: id

          collectAndMerge(mergedField.selections, typename).mapNotNull {
            val v = buildFieldOfType(path + it.responseName, stableId + it.responseName, it, resolver, map.getOrAbsent(it.responseName), it.type, customScalarAdapters)
            if (v is Optional.Absent) {
              return@mapNotNull null
            }
            it.responseName to v
          }.toMap()
        } else {
          value.value
        }
      } else {
        if (mergedField.selections.isNotEmpty()) {
          val typename = resolver.resolveTypename(FakeResolverContext(path, id, mergedField, customScalarAdapters))
          val map = mapOf("__typename" to typename)

          collectAndMerge(mergedField.selections, typename).associate {
            val fieldPath = path + it.responseName
            it.responseName to buildFieldOfType(fieldPath, fieldPath.joinToString(), it, resolver, map.getOrAbsent(it.responseName), it.type, customScalarAdapters)
          }
        } else {
          resolver.resolveLeaf(FakeResolverContext(path, id, mergedField, customScalarAdapters))
        }
      }
    }

    is CompiledNotNullType -> error("")
  }
}

/**
 * A [FakeResolver] that generates:
 * - values based on the object id hashcode for Int/Float/Boolean/ID
 * - values based on the field name for strings
 *
 * For object id, [BaseFakeResolver] uses `@typePolicy` if present. Or you can also override it by setting the
 * "__stableId" property:
 *
 * ```kotlin
 * val cat = buildCat {
 *   this["stableId"] = "foo"
 * }
 * ```
 */
open class BaseFakeResolver(private val possibleTypes: Map<String, List<String>>) : FakeResolver {
  override fun resolveLeaf(context: FakeResolverContext): Any {
    val type = context.mergedField.type.rawType()
    return when (type) {
      is CustomScalarType -> {
        when (type.name) {
          "Int" -> context.id.hashCode() % 100
          "Float" -> (context.id.hashCode() % 100000).toFloat() / 100.0
          "Boolean" -> context.id.hashCode() % 2 == 0
          "String" -> {
            val index = context.path.indexOfLast { it is String }
            context.path.subList(index, context.path.size).joinToString(separator = "") { it.toPathComponent() }
          }

          "ID" -> context.id.hashCode().absoluteValue.toString()
          else -> {
            error("Don't know how to fake custom scalar '${type.name}', override `resolveLeaf`")
          }
        }
      }
      is EnumType -> {
        type.values[context.id.hashCode().mod(type.values.size)]
      }
      else -> {
        error("This should not happen. Please file a bug if it does.")
      }
    }
  }

  private fun Any.toPathComponent(): String = when (this) {
    is Int -> "[$this]"
    else -> toString()
  }

  override fun resolveListSize(context: FakeResolverContext): Int {
    return 3
  }

  override fun resolveMaybeNull(context: FakeResolverContext): Boolean {
    return false
  }

  override fun resolveTypename(context: FakeResolverContext): String {
    val rawType = context.mergedField.type.rawType()
    if (rawType is ObjectType) {
      return rawType.name
    }

    val types = possibleTypes.get(rawType.name) ?: error("Cannot find the possible types of '${rawType.name}'")
    val index = context.id.hashCode().mod(types.size)
    return types[index]
  }

  override fun stableIdForObject(obj: Map<String, Any?>, mergedField: CompiledField): String? {
    val keyFields = mergedField.type.rawType().keyFields()

    if (obj.containsKey("__stableId")) {
      return obj.get("__stableId").toString()
    }

    if (keyFields.isNotEmpty()) {
      return buildString {
        append(obj["__typename"].toString())
        keyFields.forEach {
          append(obj[it].toString())
        }
      }
    }

    return null
  }
}

fun <T> buildData(
    adapter: Adapter<T>,
    customScalarAdapters: CustomScalarAdapters,
    map: Map<String, Any?>,
    selections: List<CompiledSelection>,
    typename: String,
    resolver: FakeResolver,
): T {
  return buildData(
      adapter,
      customScalarAdapters,
      buildFakeObject(selections, typename, map, resolver, customScalarAdapters),
  )
}

fun <T> buildData(
    adapter: Adapter<T>,
    customScalarAdapters: CustomScalarAdapters,
    map: Map<String, Any?>,
): T {
  return adapter.fromJson(
      MapJsonReader(map),
      customScalarAdapters
  )
}
