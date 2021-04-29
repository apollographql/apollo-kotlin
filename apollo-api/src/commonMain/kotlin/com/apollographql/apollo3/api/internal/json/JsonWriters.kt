package com.apollographql.apollo3.api.internal.json

import com.apollographql.apollo3.api.json.JsonWriter
import okio.Buffer
import okio.ByteString

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

inline fun buildJsonString(crossinline block: JsonWriter.() -> Unit): String {
  val buffer = Buffer()
  BufferedSinkJsonWriter(buffer).block()
  return buffer.readUtf8()
}


inline fun buildJsonByteString(crossinline block: JsonWriter.() -> Unit): ByteString {
  val buffer = Buffer()
  BufferedSinkJsonWriter(buffer).block()
  return buffer.readByteString()
}