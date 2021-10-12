package com.apollographql.apollo3.api.test

import com.apollographql.apollo3.api.CompiledType
import kotlin.reflect.KProperty


abstract class MapBuilder {
  private val map = mutableMapOf<String, Any?>()

  fun <T> resolve(responseName: String, type: CompiledType, vararg ctors: () -> Map<String, Any?>): T {
    return if (map.contains(responseName)) {
      map[responseName] as T
    } else {
      val resolver = currentTestResolver ?: error("No TestResolver found, wrap with withTestResolver() {}")
      return resolver.resolve(responseName, type, ctors)
    }
  }

  abstract fun build(): Map<String, Any?>
}

class StubbedProperty<T>(private val map: MutableMap<String, Any?>, private val responseName: String) {
  operator fun getValue(
      mapBuilder: MapBuilder,
      property: KProperty<*>,
  ): T {
    check(map.contains(responseName)) {
      "Property $responseName is not set"
    }
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

class MandatoryTypenameProperty {
  private var typename: String? = null

  operator fun getValue(
      mapBuilder: MapBuilder,
      property: KProperty<*>,
  ): String {
    check(typename != null) {
      "__typename is not known at compile-time for fallback types. Please specify it explicitely"
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