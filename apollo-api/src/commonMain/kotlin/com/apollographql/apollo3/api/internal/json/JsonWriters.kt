package com.apollographql.apollo3.api.internal.json

import com.apollographql.apollo3.api.json.JsonWriter
import okio.Buffer
import okio.ByteString
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun JsonWriter.writeObject(crossinline block: JsonWriter.() -> Unit) {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  beginObject()
  block()
  endObject()
}

@OptIn(ExperimentalContracts::class)
inline fun JsonWriter.writeArray(crossinline block: JsonWriter.() -> Unit) {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  beginArray()
  block()
  endArray()
}

@OptIn(ExperimentalContracts::class)
inline fun buildJsonString(crossinline block: JsonWriter.() -> Unit): String {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  val buffer = Buffer()
  BufferedSinkJsonWriter(buffer).block()
  return buffer.readUtf8()
}

@OptIn(ExperimentalContracts::class)
inline fun buildJsonByteString(crossinline block: JsonWriter.() -> Unit): ByteString {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  val buffer = Buffer()
  BufferedSinkJsonWriter(buffer).block()
  return buffer.readByteString()
}

@OptIn(ExperimentalContracts::class)
inline fun buildJsonMap(crossinline block: JsonWriter.() -> Unit): Any? {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  val writer = MapJsonWriter()
  writer.block()
  return writer.root()
}