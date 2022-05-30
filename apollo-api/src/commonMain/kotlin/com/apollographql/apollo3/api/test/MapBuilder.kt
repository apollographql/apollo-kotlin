package com.apollographql.apollo3.api.test

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CompiledType
import kotlin.reflect.KProperty

@DslMarker
annotation class ApolloTestBuilderMarker

/**
 * Base class for test builders that define a DSL to build type safe operation data
 */
@ApolloTestBuilderMarker
@ApolloExperimental
abstract class MapBuilder {
  /**
   * __map is public and therefore visible by callers. We prefix it with "__" to avoid the risk of mistaking it
   * with fields
   */
  protected val __map = mutableMapOf<String, Any?>()
  protected val __shouldBeAssignedFields = mutableSetOf<String>()

  fun <T> resolve(responseName: String, type: CompiledType, enumValues: List<String>, vararg ctors: () -> Map<String, Any?>): T {
    return if (__map.contains(responseName)) {
      @Suppress("UNCHECKED_CAST")
      __map[responseName] as T
    } else {
      if (__shouldBeAssignedFields.contains(responseName)) {
        error("Builder function was called but its result was not assigned to the corresponding field `$responseName` which is certainly a mistake")
      }
      val resolver = currentTestResolver ?: error("No TestResolver found, wrap with withTestResolver() {}")
      return resolver.resolve(responseName, type, enumValues, ctors)
    }
  }

  abstract fun build(): Map<String, Any?>
}

@ApolloExperimental
class StubbedProperty<T>(private val map: MutableMap<String, Any?>, private val responseName: String) {
  operator fun getValue(
      mapBuilder: MapBuilder,
      property: KProperty<*>,
  ): T {
    check(map.contains(responseName)) {
      "Property $responseName is not set"
    }
    @Suppress("UNCHECKED_CAST")
    return map[responseName] as T
  }

  operator fun setValue(
      mapBuilder: MapBuilder,
      property: KProperty<*>,
      value: T,
  ) {
    map[responseName] = value
  }
}

@ApolloExperimental
class MandatoryTypenameProperty(
    private val parentTypeName: String,
    private val possibleTypes: List<String>,
) {
  private var typename: String? = null

  operator fun getValue(
      mapBuilder: MapBuilder,
      property: KProperty<*>,
  ): String {
    check(typename != null) {
      "$parentTypeName: __typename is not known at compile-time for this type. Please specify it explicitly (allowed values: ${possibleTypes.joinToString()})"
    }
    return typename!!
  }

  operator fun setValue(
      mapBuilder: MapBuilder,
      property: KProperty<*>,
      value: String,
  ) {
    typename = value
  }
}
