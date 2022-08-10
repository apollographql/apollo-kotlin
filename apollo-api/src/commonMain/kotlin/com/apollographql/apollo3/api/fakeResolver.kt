package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.MapJsonReader

class FakeResolverContext(
    val path: List<Any>,
    val mergedField: CompiledField,
)

interface FakeResolver {
  fun resolveLeaf(context: FakeResolverContext): Any
  fun resolveListsSize(context: FakeResolverContext): Int
  fun resolveMaybeNull(context: FakeResolverContext): Boolean
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
): Map<String, Any?> {
  @Suppress("UNCHECKED_CAST")
  return buildFieldOfType(
      emptyList(),
      CompiledField.Builder("data", CompiledNotNullType(ObjectType.Builder(typename).build()))
          .selections(selections)
          .build(),
      resolver,
      Optional.Present(base),
      CompiledNotNullType(ObjectType.Builder(typename).build())
  ) as Map<String, Any?>
}

private fun buildField(
    path: List<Any>,
    mergedField: CompiledField,
    resolver: FakeResolver,
    parent: Map<String, Any?>,
): Any? {
  return buildFieldOfType(path, mergedField, resolver, parent.getOrAbsent(mergedField.responseName), mergedField.type)
}

private fun Map<String, Any?>.getOrAbsent(key: String) = if (containsKey(key)) {
  Optional.Present(get(key))
} else {
  Optional.Absent
}

private fun buildFieldOfType(
    path: List<Any>,
    mergedField: CompiledField,
    resolver: FakeResolver,
    value: Optional<Any?>,
    type: CompiledType,
): Any? {
  if (type !is CompiledNotNullType) {
    return if (value is Optional.Present) {
      if (value.value == null) {
        null
      } else {
        buildFieldOfType(path, mergedField, resolver, value, CompiledNotNullType(type))
      }
    } else {
      if (resolver.resolveMaybeNull(FakeResolverContext(path, mergedField))) {
        null
      } else {
        buildFieldOfType(path, mergedField, resolver, value, CompiledNotNullType(type))
      }
    }
  }

  return buildFieldOfNonNullType(path, mergedField, resolver, value, type.ofType)
}

private fun buildFieldOfNonNullType(
    path: List<Any>,
    mergedField: CompiledField,
    resolver: FakeResolver,
    value: Optional<Any?>,
    type: CompiledType,
): Any? {
  return when (type) {
    is CompiledListType -> {
      if (value is Optional.Present) {
        val list = (value.value as? List<Any?>) ?: error("")
        list.mapIndexed { index, item ->
          buildFieldOfType(path + index, mergedField, resolver, Optional.Present(item), type.ofType)
        }
      } else {
        0.until(resolver.resolveListsSize(FakeResolverContext(path, mergedField))).map {
          buildFieldOfType(path + it, mergedField, resolver, Optional.Absent, type.ofType)
        }
      }
    }
    is CompiledNamedType -> {
      if (value is Optional.Present) {
        if (mergedField.selections.isNotEmpty()) {
          @Suppress("UNCHECKED_CAST")
          val map = (value.value as? Map<String, Any?>) ?: error("")
          val typename = (map["__typename"] as? String) ?: error("")

          collectAndMerge(mergedField.selections, typename).associate {
            it.responseName to buildField(path + it.responseName, it, resolver, map)
          }
        } else {
          value.value
        }
      } else {
        if (mergedField.selections.isNotEmpty()) {
          val typename = resolver.resolveTypename(FakeResolverContext(path, mergedField))
          val map = mapOf("__typename" to typename)

          collectAndMerge(mergedField.selections, typename).associate {
            it.responseName to buildField(path + it.responseName, it, resolver, map)
          }
        } else {
          resolver.resolveLeaf(FakeResolverContext(path, mergedField))
        }
      }
    }
    is CompiledNotNullType -> error("")
  }
}

class DefaultFakeResolver(val types: List<CompiledNamedType>) : FakeResolver {
  private var currentInt = 0
  private var currentFloat = 0.0
  private var currentBoolean = false

  private var impl = mutableMapOf<String, Int>()

  override fun resolveLeaf(context: FakeResolverContext): Any {
    return when (val name = context.mergedField.type.leafType().name) {
      "Int" -> currentInt++
      "Float" -> currentFloat++
      "Boolean" -> (!currentBoolean).also { currentBoolean = it }
      "String" -> {
        val index = context.path.indexOfLast { it is String }
        context.path.subList(index, context.path.size).joinToString { it.toPathComponent() }
      }

      "ID" -> context.path.joinToString { it.toString() }
      else -> {
        val type = (types.find { it.name == name } as? EnumType) ?: error("Don't know how to instantiate leaf $name")
        val index = impl.getOrElse(name) { 0 }

        impl[name] = index + 1

        type.values[index % type.values.size]
      }
    }
  }

  private fun Any.toPathComponent(): String = when (this) {
    is Int -> "[$this]"
    else -> toString()
  }

  override fun resolveListsSize(context: FakeResolverContext): Int {
    return 3
  }

  override fun resolveMaybeNull(context: FakeResolverContext): Boolean {
    return false
  }

  override fun resolveTypename(context: FakeResolverContext): String {
    val leafType = context.mergedField.type.leafType()
    val name = leafType.name
    val index = impl.getOrElse(name) { 0 }

    impl[name] = index + 1

    // XXX: Cache this computation
    val possibleTypes = possibleTypes(types, leafType)
    return possibleTypes[index % possibleTypes.size].name
  }
}

fun <T> buildData(
    adapter: Adapter<T>,
    selections: List<CompiledSelection>,
    typename: String,
    map: Map<String, Any?>,
    resolver: FakeResolver
    ): T {
  return adapter.obj(false).fromJson(
      MapJsonReader(
          buildFakeObject(selections, typename, map, resolver)
      ),
      CustomScalarAdapters.Unsafe
  )
}
