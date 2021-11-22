package com.apollographql.apollo3.api.json.internal

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter
import com.apollographql.apollo3.api.json.JsonWriter
import okio.Buffer
import okio.ByteString
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

@OptIn(ExperimentalContracts::class)
@ApolloInternal
@JvmName("-writeObject")
inline fun JsonWriter.writeObject(crossinline block: JsonWriter.() -> Unit) {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  beginObject()
  block()
  endObject()
}

@OptIn(ExperimentalContracts::class)
@ApolloInternal
@JvmName("-writeArray")
inline fun JsonWriter.writeArray(crossinline block: JsonWriter.() -> Unit) {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  beginArray()
  block()
  endArray()
}

@OptIn(ExperimentalContracts::class)
@ApolloInternal
@JvmName("-buildJsonString")
inline fun buildJsonString(crossinline block: JsonWriter.() -> Unit): String {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  val buffer = Buffer()
  BufferedSinkJsonWriter(buffer).block()
  return buffer.readUtf8()
}

@OptIn(ExperimentalContracts::class)
@ApolloInternal
@JvmName("-buildJsonByteString")
inline fun buildJsonByteString(crossinline block: JsonWriter.() -> Unit): ByteString {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  val buffer = Buffer()
  BufferedSinkJsonWriter(buffer).block()
  return buffer.readByteString()
}

@OptIn(ExperimentalContracts::class)
@ApolloInternal
@JvmName("-buildJsonMap")
inline fun buildJsonMap(crossinline block: JsonWriter.() -> Unit): Any? {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  val writer = MapJsonWriter()
  writer.block()
  return writer.root()
}