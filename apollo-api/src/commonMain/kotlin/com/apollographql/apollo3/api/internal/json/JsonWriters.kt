package com.apollographql.apollo3.api.internal.json

import com.apollographql.apollo3.api.json.JsonWriter

inline fun JsonWriter.writeObject(crossinline block: JsonWriter.() -> Unit) {
  beginObject()
  block()
  endObject()
}

inline fun JsonWriter.writeArray(crossinline block: JsonWriter.() -> Unit) {
  beginArray()
  block()
  endArray()
}