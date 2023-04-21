package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.json.MapJsonWriter

@Suppress("PropertyName")
abstract class ObjectBuilder<out T : Map<String, Any?>>(val customScalarAdapters: CustomScalarAdapters) : BuilderScope {
  val __fields = mutableMapOf<String, Any?>()

  var __typename: String by __fields

  operator fun set(key: String, value: Any?) {
    __fields[key] = value
  }

  abstract fun build(): T
}

interface BuilderScope {
  val scalarAdapters: CustomScalarAdapters
}

interface BuilderFactory<out T> {
  fun newBuilder(customScalarAdapters: CustomScalarAdapters): T
}

fun Builder(customScalarAdapters: CustomScalarAdapters): BuilderScope {
  return object : BuilderScope {
    val customScalarAdapters: CustomScalarAdapters
      get() = customScalarAdapters
  }
}

val GlobalBuilder = object : BuilderScope {
  val customScalarAdapters: CustomScalarAdapters
    get() = CustomScalarAdapters.PassThrough
}

/**
 * A property delegate that stores the given property as it would be serialized in a Json
 * This is needed in Data Builders because the serializer only work from Json
 */
class BuilderProperty<T>(val adapter: DataAdapter<T>) {
  operator fun getValue(thisRef: ObjectBuilder<*>, property: kotlin.reflect.KProperty<*>): T {
    // XXX: remove this cast as MapJsonReader can tak any value
    @Suppress("UNCHECKED_CAST")
    val data = thisRef.__fields[property.name] as Map<String, Any?>
    return adapter.deserializeData(MapJsonReader(data), DataAdapter.DeserializeDataContext(customScalarAdapters = CustomScalarAdapters.Empty, falseBooleanVariables = emptySet(), mergedDeferredFragmentIds = null))
  }

  operator fun setValue(thisRef: ObjectBuilder<*>, property: kotlin.reflect.KProperty<*>, value: T) {
    thisRef.__fields[property.name] = MapJsonWriter().apply {
      adapter.toJson(this, CustomScalarAdapters.Empty, value)
    }.root()
  }
}

fun <T> adaptValue(adapter: DataAdapter<T>, value: T): Any? {
  return MapJsonWriter().apply {
    adapter.toJson(this, CustomScalarAdapters.Empty, value)
  }.root()
}

abstract class ObjectMap(__fields: Map<String, Any?>) : Map<String, Any?> by __fields
