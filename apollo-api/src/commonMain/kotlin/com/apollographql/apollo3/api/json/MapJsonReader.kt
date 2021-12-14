package com.apollographql.apollo3.api.json

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader.Companion.MAX_STACK_SIZE
import com.apollographql.apollo3.api.json.internal.toDoubleExact
import com.apollographql.apollo3.api.json.internal.toIntExact
import com.apollographql.apollo3.api.json.internal.toLongExact

/**
 * A [JsonReader] that reads data from a regular [Map<String, Any?>]
 *
 * Map values should be any of:
 * - String
 * - Int
 * - Double
 * - Long
 * - JsonNumber
 * - null
 * - Map<String, Any?> where values are any of these values recursively
 * - List<Any?> where values are any of these values recursively
 *
 * Anything else is undefined
 *
 * @param root the root [Map] to read from
 *
 * Because it's mostly used internally the checks/verifications are very slim.
 * To read from a [okio.BufferedSource], see also [BufferedSourceJsonReader]
 */
class MapJsonReader(val root: Map<String, Any?>) : JsonReader {
  // Like a Map but easier to access sequentially
  // Could be optimized
  private class OrderedMap(val entries: List<Entry>)
  private class Entry(val key: String, val value: Any?)

  private val dataStack = arrayOfNulls<Any>(MAX_STACK_SIZE)
  private val indexStack = IntArray(MAX_STACK_SIZE)
  private val nameIndexStack = IntArray(MAX_STACK_SIZE)

  private var stackSize = 0

  // Will be non-null if a name has been read and is reset when a value is read
  // No need to stack this, when we pop, we know we have to read a new name
  private var pendingName: String? = null

  init {
    /**
     * We only allow Maps at the root of the Json. Initialize the state accordingly
     */
    push(OrderedMap(listOf(Entry("root", root))))
    pendingName = "root"
  }

  private fun push(data: Any) {
    check(stackSize < MAX_STACK_SIZE) {
      "Nesting too deep"
    }

    dataStack[stackSize] = data
    indexStack[stackSize] = 0
    nameIndexStack[stackSize] = 0

    stackSize++
    pendingName = null
  }

  private fun pop() {
    // Allow garbage collection
    dataStack[stackSize] = null
    stackSize--
    pendingName = null
  }

  private fun nextValue(): Any? {
    return when (val data = dataStack[stackSize - 1]) {
      is List<*> -> {
        data[indexStack[stackSize - 1]++]
      }
      is OrderedMap -> {
        check(pendingName != null)
        pendingName = null
        data.entries[indexStack[stackSize - 1]++].value
      }
      else -> error("")
    }
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

  private fun anyToToken(any: Any?) = when (any) {
    null -> JsonReader.Token.NULL
    is List<*> -> JsonReader.Token.BEGIN_ARRAY
    is Map<*, *> -> JsonReader.Token.BEGIN_OBJECT
    is Int -> JsonReader.Token.NUMBER
    is Long -> JsonReader.Token.NUMBER
    is Double -> JsonReader.Token.NUMBER
    is String -> JsonReader.Token.STRING
    is Boolean -> JsonReader.Token.BOOLEAN
    else -> error("Unsupported value $any")
  }

  override fun hasNext(): Boolean {
    return when (val data = dataStack[stackSize - 1]) {
      is List<*> -> {
        indexStack[stackSize - 1] < data.size
      }
      is OrderedMap -> {
        indexStack[stackSize - 1] < data.entries.size
      }
      else -> error("")
    }
  }

  override fun peek(): JsonReader.Token {
    if (stackSize == 1 && indexStack[0] == 1) {
      return JsonReader.Token.END_DOCUMENT
    }
    return when (val data = dataStack[stackSize - 1]) {
      is List<*> -> {
        if (indexStack[stackSize - 1] < data.size) {
          anyToToken(data[indexStack[stackSize - 1]])
        } else {
          JsonReader.Token.END_ARRAY
        }
      }
      is OrderedMap -> {
        if (indexStack[stackSize - 1] < data.entries.size) {
          if (pendingName == null) {
            JsonReader.Token.NAME
          } else {
            check(pendingName == data.entries[indexStack[stackSize - 1]].key)
            val value = data.entries[indexStack[stackSize - 1]].value
            anyToToken(value)
          }
        } else {
          JsonReader.Token.END_OBJECT
        }
      }
      else -> error("")
    }
  }

  override fun nextName(): String {
    val data = dataStack[stackSize - 1]
    check(data is OrderedMap)
    check(pendingName == null)
    pendingName = data.entries[indexStack[stackSize - 1]].key
    return pendingName!!
  }

  override fun nextString(): String? {
    // nextValue can be an Int or Double too
    return nextValue()?.toString()
  }

  override fun nextBoolean(): Boolean {
    return nextValue() as Boolean
  }

  override fun nextNull(): Nothing? {
    nextValue().also {
      check(it == null)
    }
    return null
  }

  override fun nextDouble(): Double {
    return when (val value = nextValue()) {
      is Int -> value.toDouble()
      is Long -> value.toDoubleExact()
      is Double -> value
      is String -> value.toDouble()
      is JsonNumber -> value.value.toDouble()
      else -> error("Expected Double but got $value instead")
    }
  }

  override fun nextInt(): Int {
    return when (val value = nextValue()) {
      is Int -> value
      is Long -> value.toIntExact()
      is Double -> value.toIntExact()
      is String -> value.toInt()
      is JsonNumber -> value.value.toInt()
      else -> error("Expected Int but got $value instead")
    }
  }

  override fun nextLong(): Long {
    return when (val value = nextValue()) {
      is Int -> value.toLong()
      is Long -> value
      is Double -> value.toLongExact()
      is String -> value.toLong()
      is JsonNumber -> value.value.toLong()
      else -> error("Expected Int but got $value instead")
    }
  }

  override fun nextNumber(): JsonNumber {
    return when (val value = nextValue()) {
      is Int, is Long, is Double -> JsonNumber(value.toString())
      is String -> JsonNumber(value) // assert value is a valid number
      is JsonNumber -> value
      else -> error("Expected JsonNumber but got $value instead")
    }
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
      val expectedIndex = nameIndexStack[stackSize - 1]
      if (names[expectedIndex] == name) {
        return expectedIndex.also {
          nameIndexStack[stackSize - 1] = expectedIndex + 1
          if (nameIndexStack[stackSize - 1] == names.size) {
            nameIndexStack[stackSize - 1] = 0
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
              nameIndexStack[stackSize - 1] = index + 1
              if (nameIndexStack[stackSize - 1] == names.size) {
                nameIndexStack[stackSize - 1] = 0
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
  override fun rewind() {
    indexStack[stackSize - 1] = 0
    nameIndexStack[stackSize - 1] = 0
  }

  companion object {

    /**
     * buffers the next Object. Has to be called in `BEGIN_OBJECT` position.
     * The returned [MapJsonReader] can use [MapJsonReader.rewind] to read fields
     * multiple times
     */
    fun JsonReader.buffer(): MapJsonReader {
      if (this is MapJsonReader) return this

      val token = this.peek()
      check(token == JsonReader.Token.BEGIN_OBJECT) {
        "Failed to buffer json reader, expected `BEGIN_OBJECT` but found `$token` json token"
      }

      @Suppress("UNCHECKED_CAST")
      @OptIn(ApolloInternal::class)
      val data = this.readAny() as Map<String, Any?>
      return MapJsonReader(data)
    }
  }
}
