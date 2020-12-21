package com.apollographql.apollo.api.internal.json

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