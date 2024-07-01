@file:JvmName("-JsonWriters")

package com.apollographql.apollo.api.json

import okio.Buffer
import okio.ByteString
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

fun JsonWriter.writeAny(value: Any?) {
  when (value) {
    null -> nullValue()

    is Map<*, *> -> {
      writeObject {
        value.forEach { (key, value) ->
          name(key.toString())
          writeAny(value)
        }
      }
    }

    is List<*> -> {
      writeArray {
        value.forEach {
          writeAny(it)
        }
      }
    }

    is Boolean -> value(value)
    is Int -> value(value)
    is Long -> value(value)
    is Double -> value(value)
    is JsonNumber -> value(value)
    is String -> value(value)
    else -> error("Cannot write $value of class '${value::class}' to Json")
  }
}

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
inline fun buildJsonString(indent: String? = null, crossinline block: JsonWriter.() -> Unit): String {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  val buffer = Buffer()
  BufferedSinkJsonWriter(buffer, indent).block()
  return buffer.readUtf8()
}

@OptIn(ExperimentalContracts::class)
inline fun buildJsonByteString(indent: String? = null, crossinline block: JsonWriter.() -> Unit): ByteString {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }

  val buffer = Buffer()
  BufferedSinkJsonWriter(buffer, indent).block()
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