package com.apollographql.apollo3.api.internal.json

import com.apollographql.apollo3.api.internal.json.Utils.readRecursively
import com.apollographql.apollo3.api.json.JsonReader

/**
 * A [JsonReader] that reads data from a regular [Map<String, Any?>]
 *
 * Map values should be any of:
 * - String
 * - Int
 * - Double
 * - null
 * - Map<String, Any?> where values are any of these values recursively
 * - List<Any?> where values are any of these values recursively
 *
 * Anything else is undefined
 *
 * @param root the root [Map] to read from
 *
 * The base implementation was taken from Moshi and ported to Kotlin multiplatform with some tweaks to make it better suited for GraphQL
 * (see [JsonReader]).
 *
 * To read from a [okio.BufferedSource], see also [BufferedSourceJsonReader]
 */
class MapJsonReader(val root: Map<String, Any?>) : JsonReader {
  // Like a Map but easier to access sequentially
  private class OrderedMap(val entries: List<Entry>)
  private class Entry(val key: String, val value: Any?)

  private val dataStack = ArrayList<Any>()
  private val indexStack = ArrayList<Int>()

  // A simple holder to kick things off
  private val sentinel = OrderedMap(listOf(Entry("root", root)))

  // Either a List or an [OrderedMap]
  private var currentData: Any = sentinel
  // The current index in the List or [OrderedMap]
  private var currentIndex = 0
  // Will be non-null if a name has been read
  // No need to stack this, when we pop, we know we have to read a new name
  private var currentName: String? = "root"

  /**
   * See [com.apollographql.apollo3.api.internal.json.BufferedSourceJsonReader] for the 32 limitation
   */
  private val nameIndexStack = IntArray(32).apply {
    this[0] = 0
  }

  private var nameIndexStackSize = 1

  private fun push(data: Any) {
    dataStack.add(currentData)
    indexStack.add(currentIndex)

    currentData = data
    currentIndex = 0
    currentName = null
  }

  private fun pop() {
    currentData = dataStack.removeAt(dataStack.size - 1)
    currentIndex = indexStack.removeAt(indexStack.size - 1)
    currentName = null
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

    nameIndexStackSize++
    check(nameIndexStackSize < 33) {
      "Json is too deeply nested"
    }
    nameIndexStack[nameIndexStackSize - 1] = 0
  }

  override fun endObject() = apply {
    pop()
    nameIndexStackSize--
  }

  private fun anyToToken(any: Any?) = when (any) {
    null -> JsonReader.Token.NULL
    is List<*> -> JsonReader.Token.BEGIN_ARRAY
    is Map<*, *> -> JsonReader.Token.BEGIN_OBJECT
    is Int -> JsonReader.Token.NUMBER
    is Double -> JsonReader.Token.NUMBER
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
    // nextValue can be an Int or Double too
    return nextValue()?.toString()
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
    return when (val value = nextValue()) {
      is Double -> value
      // Coerce Int to Double https://spec.graphql.org/draft/#sec-Float.Result-Coercion
      is Int -> value.toDouble()
      else -> error("Cannot coerce $value to Double")
    }
  }

  override fun nextInt(): Int {
    return nextValue() as Int
  }

  override fun skipValue() {
    nextValue()
  }

  override fun close() {
  }

  override fun selectName(names: List<String>): Int {
    if (names.isEmpty()) {
      return -1
    }

    while (hasNext()) {
      val name = nextName()
      val expectedIndex = nameIndexStack[nameIndexStackSize - 1]
      if (names[expectedIndex] == name) {
        return expectedIndex.also {
          nameIndexStack[nameIndexStackSize - 1] = expectedIndex + 1
          if (nameIndexStack[nameIndexStackSize - 1] == names.size) {
            nameIndexStack[nameIndexStackSize - 1] = 0
          }
        }
      } else {
        // guess failed, fallback to full search
        var index = expectedIndex
        while (true) {
          index++
          if (index == names.size) {
            index = 0
          }
          if (index == expectedIndex) {
            break
          }
          if (names[index] == name) {
            return index.also {
              nameIndexStack[nameIndexStackSize - 1] = index + 1
              if (nameIndexStack[nameIndexStackSize - 1] == names.size) {
                nameIndexStack[nameIndexStackSize - 1] = 0
              }
            }
          }
        }

        skipValue()
      }
    }
    return -1
  }

  /**
   * Rewinds to the beginning of the current object.
   */
  fun rewind() {
    currentIndex = 0
    currentName = null
  }

  companion object {

    /**
     * buffers the next Object. Has to be called in `BEGIN_OBJECT` position.
     * The return [MapJsonReader] can use [MapJsonReader.rewind] to read fields
     * multiple times
     */
    fun JsonReader.buffer(): MapJsonReader {
      if (this is MapJsonReader) return this

      val token = this.peek()
      check(token == JsonReader.Token.BEGIN_OBJECT) {
        "Failed to buffer json reader, expected `BEGIN_OBJECT` but found `$token` json token"
      }

      @Suppress("UNCHECKED_CAST")
      val data = this.readRecursively() as Map<String, Any?>
      return MapJsonReader(data)
    }
  }
}
