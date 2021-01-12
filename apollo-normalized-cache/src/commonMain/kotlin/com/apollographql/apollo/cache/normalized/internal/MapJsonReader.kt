package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.BigDecimal
import com.apollographql.apollo.api.internal.json.JsonReader

class MapJsonReader(val root: Map<String, Any?>) : JsonReader {
  class OrderedMap(val entries: List<Entry>)
  class Entry(val key: String, val value: Any?)

  val dataStack = ArrayList<Any>()
  val indexStack = ArrayList<Int>()
  val nameStack = ArrayList<String?>()

  val sentinel = OrderedMap(listOf(Entry("root", root)))

  var currentData: Any = sentinel
  var currentIndex = 0
  var currentName: String? = "root"

  private fun push(data: Any) {
    dataStack.add(currentData)
    indexStack.add(currentIndex)
    nameStack.add(currentName)

    currentData = data
    currentIndex = 0
    currentName = null
  }

  private fun pop() {
    currentData = dataStack.removeAt(dataStack.size - 1)
    currentIndex = indexStack.removeAt(indexStack.size - 1)
    currentName = nameStack.removeAt(nameStack.size - 1)
  }

  private fun nextValue() = when (val data = currentData) {
    is List<*> -> {
      val value = data[currentIndex]
      currentIndex++
      value
    }
    is OrderedMap -> {
      check(currentName == data.entries[currentIndex].key)
      val value = data.entries[currentIndex].value
      currentName = null
      currentIndex++
      value
    }
    else -> error("")
  }

  override fun beginArray() = apply {
    val list = nextValue()
    check(list is List<*>)
    push(list)
  }

  override fun endArray() = apply {
    pop()
  }

  override fun beginObject() = apply {
    val map = nextValue()
    check(map is Map<*, *>)
    push(OrderedMap(map.entries.map { Entry(it.key as String, it.value) }))
  }

  override fun endObject() = apply {
    pop()
  }

  private fun anyToToken(any: Any?) = when(any) {
    null -> JsonReader.Token.NULL
    is List<*> -> JsonReader.Token.BEGIN_ARRAY
    is Map<*, *> -> JsonReader.Token.BEGIN_OBJECT
    is BigDecimal -> JsonReader.Token.NUMBER
    is String -> JsonReader.Token.STRING
    is Boolean -> JsonReader.Token.BOOLEAN
    else -> error("")
  }
  override fun hasNext(): Boolean {
    return when (val data = currentData) {
      is List<*> -> {
        currentIndex < data.size
      }
      is OrderedMap -> {
        currentIndex < data.entries.size
      }
      else -> error("")
    }
  }

  override fun peek(): JsonReader.Token {
    return when (val data = currentData) {
      is List<*> -> {
        if (currentIndex < data.size) {
          anyToToken(data[currentIndex])
        } else {
          JsonReader.Token.END_ARRAY
        }
      }
      is OrderedMap -> {
        if (currentIndex < data.entries.size) {
          if (currentName == null) {
            JsonReader.Token.NAME
          } else {
            check(currentName == data.entries[currentIndex].key)
            val value = data.entries[currentIndex].value
            anyToToken(value)
          }
        } else {
          if (currentName == null && data == sentinel) {
            JsonReader.Token.END_DOCUMENT
          } else {
            JsonReader.Token.END_OBJECT
          }
        }
      }
      else -> error("")
    }
  }

  override fun nextName(): String {
    val data = currentData
    check(data is OrderedMap)
    check(currentName == null)
    currentName = data.entries[currentIndex].key
    return currentName!!
  }

  override fun nextString(): String? {
    return nextValue() as String?
  }

  override fun nextBoolean(): Boolean {
    return nextValue() as Boolean
  }

  override fun <T> nextNull(): T? {
    nextValue().also {
      check(it == null)
    }
    return null
  }

  override fun nextDouble(): Double {
    return nextValue() as Double
  }

  override fun nextLong(): Long {
    return nextValue() as Long
  }

  override fun nextInt(): Int {
    return nextValue() as Int
  }

  override fun skipValue() {
    nextValue()
  }

  override fun close() {
  }
}