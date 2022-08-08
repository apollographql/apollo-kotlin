package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.json.MapJsonReader
import com.apollographql.apollo3.api.json.MapJsonWriter

@Suppress("PropertyName")
abstract class ObjectBuilder {
  val __fields = mutableMapOf<String, Any?>()

  var __typename: String by __fields

  operator fun set(key: String, value: Any) {
    __fields[key] = value
  }
}

/**
 * A property delegate that stores the given property as it would be serialized in a Json
 * This is needed in Data Builders because the serializer only work from Json
 */
class BuilderProperty<T>(val adapter: Adapter<T>) {
  operator fun getValue(thisRef: ObjectBuilder, property: kotlin.reflect.KProperty<*>): T {
    // XXX: remove this cast as MapJsonReader can tak any value
    @Suppress("UNCHECKED_CAST")
    val data = thisRef.__fields[property.name] as Map<String, Any?>
    return adapter.fromJson(MapJsonReader(data), CustomScalarAdapters.Empty)
  }

  operator fun setValue(thisRef: ObjectBuilder, property: kotlin.reflect.KProperty<*>, value: T) {

    thisRef.__fields[property.name] = MapJsonWriter().apply {
      adapter.toJson(this, CustomScalarAdapters.Empty, value)
    }.root()
  }
}


