package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.MapJsonReader

/**
 * @property path the path to the element being resolved. [path] is a list of [String] or [Int]
 * @property mergedField the field being resolved
 */
class FakeResolverContext internal constructor(
    val path: List<Any>,
    val cacheKey: String,
    val mergedField: CompiledField,
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
 * @param selections: the selections of the operation to fake
 * @param typename: the type of the object currently being resolved. Always an object type
 * @param base:
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

private fun buildField(
    path: List<Any>,
    cacheKey: String,
    mergedField: CompiledField,
    resolver: FakeResolver,
    parent: Map<String, Any?>,
    customScalarAdapters: CustomScalarAdapters,
): Any? {
  return buildFieldOfType(path, cacheKey, mergedField, resolver, parent.getOrAbsent(mergedField.responseName), mergedField.type, customScalarAdapters)
}

private fun Map<String, Any?>.getOrAbsent(key: String) = if (containsKey(key)) {
  Optional.Present(get(key))
} else {
  Optional.Absent
}

private fun buildFieldOfType(
    path: List<Any>,
    cacheKey: String,
    mergedField: CompiledField,
    resolver: FakeResolver,
    value: Optional<Any?>,
    type: CompiledType,
    customScalarAdapters: CustomScalarAdapters,
): Any? {
  if (type !is CompiledNotNullType) {
    return if (value is Optional.Present) {
      if (value.value == null) {
        null
      } else {
        buildFieldOfType(path, cacheKey, mergedField, resolver, value, CompiledNotNullType(type), customScalarAdapters)
      }
    } else {
      if (resolver.resolveMaybeNull(FakeResolverContext(path, cacheKey, mergedField))) {
        null
      } else {
        buildFieldOfType(path, cacheKey, mergedField, resolver, value, CompiledNotNullType(type), customScalarAdapters)
      }
    }
  }

  return buildFieldOfNonNullType(path, cacheKey, mergedField, resolver, value, type.ofType, customScalarAdapters)
}

private fun buildFieldOfNonNullType(
    path: List<Any>,
    cacheKey: String,
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
          buildFieldOfType(path + index, cacheKey, mergedField, resolver, Optional.Present(item), type.ofType, customScalarAdapters)
        }
      } else {
        0.until(resolver.resolveListSize(FakeResolverContext(path, cacheKey, mergedField))).map {
          buildFieldOfType(path + it, cacheKey + it, mergedField, resolver, Optional.Absent, type.ofType, customScalarAdapters)
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
           * is present because it was created as a concrete type
           */
          val typename = (map["__typename"] as? String) ?: error("")

          /**
           * If this object has a cacheKey, we can use it so that the same object at different paths returns the same fake values
           */
          val keyFields = mergedField.type.rawType().keyFields()
          val keyValues = keyFields.mapNotNull {keyField ->
            if (map.containsKey(keyField)) {
              map[keyField].toString()
            } else {
              null
            }
          }

          val newCacheKey = if (keyValues.isNotEmpty()) {
            typename + keyValues.joinToString()
          } else {
            cacheKey
          }
          collectAndMerge(mergedField.selections, typename).associate {
            it.responseName to buildField(path + it.responseName, newCacheKey + it.responseName, it, resolver, map, customScalarAdapters)
          }
        } else {
          value.value
        }
      } else {
        if (mergedField.selections.isNotEmpty()) {
          val typename = resolver.resolveTypename(FakeResolverContext(path, cacheKey, mergedField))
          val map = mapOf("__typename" to typename)

          collectAndMerge(mergedField.selections, typename).associate {
            val fieldPath = path + it.responseName
            it.responseName to buildField(fieldPath, fieldPath.joinToString(), it, resolver, map, customScalarAdapters)
          }
        } else {
          val leafValue = resolver.resolveLeaf(FakeResolverContext(path, cacheKey, mergedField))
          if (type is CustomScalarType) {
            /**
             * This might be a scalar with a build time registered adapter.
             * If that's the case, we need to adapt before putting the value in the map
             * so that subsequent adapters can deserialize
             */
            val adapter = try {
              customScalarAdapters.responseAdapterFor<Any>(type)
            } catch (e: Exception) {
              null
            }
            if (adapter != null) {
              adaptValue(adapter, leafValue)
            } else {
              leafValue
            }
          } else {
            leafValue
          }
        }
      }
    }

    is CompiledNotNullType -> error("")
  }
}

open class DefaultFakeResolver(types: List<CompiledNamedType>) : FakeResolver {
  private val enumTypes = types.filterIsInstance<EnumType>()
  private val allTypes = types

  override fun resolveLeaf(context: FakeResolverContext): Any {
    return when (val name = context.mergedField.type.rawType().name) {
      "Int" -> context.cacheKey.hashCode() % 100
      "Float" -> (context.cacheKey.hashCode() % 100000).toFloat() / 100.0
      "Boolean" -> context.cacheKey.hashCode() % 2 == 0
      "String" -> {
        val index = context.path.indexOfLast { it is String }
        context.path.subList(index, context.path.size).joinToString(separator = "") { it.toPathComponent() }
      }

      "ID" -> context.path.joinToString(separator = "") { it.toPathComponent() }
      else -> {
        val type = enumTypes.find { it.name == name } ?: error("Don't know how to instantiate leaf $name")

        type.values[context.cacheKey.hashCode().mod(type.values.size)]
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

    // XXX: Cache this computation
    val possibleTypes = possibleTypes(allTypes, rawType)
    val index = context.cacheKey.hashCode().mod(possibleTypes.size)
    return possibleTypes[index].name
  }
}

fun <T> buildData(
    adapter: Adapter<T>,
    selections: List<CompiledSelection>,
    typename: String,
    map: Map<String, Any?>,
    resolver: FakeResolver,
    customScalarAdapters: CustomScalarAdapters,
): T {
  return adapter.obj(false).fromJson(
      MapJsonReader(buildFakeObject(selections, typename, map, resolver, customScalarAdapters)),
      CustomScalarAdapters.PassThrough
  )
}

fun <T> buildFragmentData(
    adapter: Adapter<T>,
    selections: List<CompiledSelection>,
    typename: String,
    block: Any? = null,
    resolver: FakeResolver,
    type: CompiledType,
    customScalarAdapters: CustomScalarAdapters,
): T {
  val map = if (block == null) {
    mapOf(
        "__typename" to resolver.resolveTypename(
            FakeResolverContext(
                emptyList(),
                "fragmentRoot",
                CompiledField.Builder("__fragmentRoot", type).build()
            )
        )
    )
  } else {
    @Suppress("UNCHECKED_CAST")
    block as (BuilderScope.() -> Map<String, Any?>)
    block.invoke(GlobalBuilder)
  }

  return buildData(
      adapter,
      selections,
      typename,
      map,
      resolver,
      customScalarAdapters
  )
}
