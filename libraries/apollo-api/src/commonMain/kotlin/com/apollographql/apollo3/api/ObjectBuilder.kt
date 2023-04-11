package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.json.MapJsonWriter

@Suppress("PropertyName")
abstract class ObjectBuilder<out T: Map<String, Any?>>(override val scalarAdapters: ScalarAdapters): BuilderScope {
  val __fields = mutableMapOf<String, Any?>()

  var __typename: String by __fields

  operator fun set(key: String, value: Any?) {
    __fields[key] = value
  }

  abstract fun build(): T
}

interface BuilderScope {
  val scalarAdapters: ScalarAdapters
}

interface BuilderFactory<out T> {
  fun newBuilder(scalarAdapters: ScalarAdapters) : T
}

fun Builder(scalarAdapters: ScalarAdapters): BuilderScope {
  return object : BuilderScope {
    override val scalarAdapters: ScalarAdapters
      get() = scalarAdapters
  }
}

val GlobalBuilder = object : BuilderScope {
  override val scalarAdapters: ScalarAdapters
    get() = ScalarAdapters.PassThrough
}

/**
 * A property delegate that stores the given property as it would be serialized in a Json
 * This is needed in Data Builders because the serializer only work from Json
 */
class BuilderProperty<T>(val adapter: ApolloAdapter<T>) {
  operator fun getValue(thisRef: ObjectBuilder<*>, property: kotlin.reflect.KProperty<*>): T {
    // XXX: remove this cast as MapJsonReader can tak any value
    @Suppress("UNCHECKED_CAST")
    val data = thisRef.__fields[property.name] as Map<String, Any?>
    return adapter.fromJson(MapJsonReader(data), ScalarAdapters.Empty)
  }

  operator fun setValue(thisRef: ObjectBuilder<*>, property: kotlin.reflect.KProperty<*>, value: T) {
    thisRef.__fields[property.name] = MapJsonWriter().apply {
      adapter.toJson(this, ScalarAdapters.Empty, value)
    }.root()
  }
}

fun <T> adaptValue(adapter: ApolloAdapter<T>, value: T): Any? {
  return MapJsonWriter().apply {
    adapter.toJson(this, ScalarAdapters.Empty, value)
  }.root()
}

abstract class ObjectMap(__fields: Map<String, Any?>): Map<String, Any?> by __fields
