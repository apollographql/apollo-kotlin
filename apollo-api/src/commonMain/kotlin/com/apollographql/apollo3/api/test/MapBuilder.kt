package com.apollographql.apollo3.api.test

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CompiledType
import kotlin.reflect.KProperty

/**
 * Base class for test builders that define a DSL to build type safe operation data
 */
@ApolloExperimental
abstract class MapBuilder {
  /**
   * __map is public and therefore visible by callers. We prefix it with "__" to avoid the risk of mistaking it
   * with fields
   */
  protected val __map = mutableMapOf<String, Any?>()

  fun <T> resolve(responseName: String, type: CompiledType, vararg ctors: () -> Map<String, Any?>): T {
    return if (__map.contains(responseName)) {
      @Suppress("UNCHECKED_CAST")
      __map[responseName] as T
    } else {
      val resolver = currentTestResolver ?: error("No TestResolver found, wrap with withTestResolver() {}")
      return resolver.resolve(responseName, type, ctors)
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
class MandatoryTypenameProperty {
  private var typename: String? = null

  operator fun getValue(
      mapBuilder: MapBuilder,
      property: KProperty<*>,
  ): String {
    check(typename != null) {
      "__typename is not known at compile-time for this type. Please specify it explicitely"
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