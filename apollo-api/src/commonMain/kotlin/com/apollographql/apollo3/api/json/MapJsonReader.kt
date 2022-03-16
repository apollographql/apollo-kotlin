package com.apollographql.apollo3.api.json

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader.Companion.MAX_STACK_SIZE
import com.apollographql.apollo3.api.json.internal.toDoubleExact
import com.apollographql.apollo3.api.json.internal.toIntExact
import com.apollographql.apollo3.api.json.internal.toLongExact
import com.apollographql.apollo3.exception.JsonDataException

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
 * To read from a [okio.BufferedSource], see also [BufferedSourceJsonReader]
 */
class MapJsonReader(private val root: Map<String, Any?>) : JsonReader {

  private var peekedToken: JsonReader.Token = JsonReader.Token.END_OBJECT
  private var peekedData: Any? = null
  private val path = arrayOfNulls<Any>(MAX_STACK_SIZE)
  private val iteratorStack = arrayOfNulls<Iterator<*>>(MAX_STACK_SIZE)

  private var stackSize = 0

  private fun reset() {
    root.entries
    peekedToken = JsonReader.Token.BEGIN_OBJECT
    peekedData = root
  }

  init {
    reset()
  }

  private fun anyToToken(any: Any?) = when (any) {
    null -> JsonReader.Token.NULL
    is List<*> -> JsonReader.Token.BEGIN_ARRAY
    is Map<*, *> -> JsonReader.Token.BEGIN_OBJECT
    is Int -> JsonReader.Token.NUMBER
    is Long -> JsonReader.Token.NUMBER
    is Double -> JsonReader.Token.NUMBER
    is JsonNumber -> JsonReader.Token.NUMBER
    is String -> JsonReader.Token.STRING
    is Boolean -> JsonReader.Token.BOOLEAN
    else -> error("Unsupported value $any")
  }

  /**
   * Updates the current token and data
   *
   * Requires iteratorStack and indexStack
   */
  private fun advanceIterator() {
    if (stackSize == 0) {
      peekedToken = JsonReader.Token.END_DOCUMENT
      return
    }

    val currentIterator = iteratorStack[stackSize - 1]!!

    if (currentIterator.hasNext()) {
      val next = currentIterator.next()
      peekedData = next

      if (path[stackSize - 1] is Int) {
        path[stackSize - 1] = (path[stackSize - 1] as Int) + 1
      }

      peekedToken = when (next) {
        is Map.Entry<*, *> -> JsonReader.Token.NAME
        else -> anyToToken(next)
      }
    } else {
      peekedToken = if (path[stackSize - 1] is Int) {
        JsonReader.Token.END_ARRAY
      } else {
        JsonReader.Token.END_OBJECT
      }
    }
  }

  override fun beginArray() = apply {
    if (peek() != JsonReader.Token.BEGIN_ARRAY) {
      throw JsonDataException("Expected BEGIN_ARRAY but was ${peek()} at path ${getPath()}")
    }

    val currentValue = peekedData as List<Any?>

    stackSize++
    path[stackSize - 1] = 0
    iteratorStack[stackSize - 1] = currentValue.iterator()
    advanceIterator()
  }

  override fun endArray() = apply {
    if (peek() != JsonReader.Token.END_ARRAY) {
      throw JsonDataException("Expected END_ARRAY but was ${peek()} at path ${getPath()}")
    }
    stackSize--
    iteratorStack[stackSize] = null // allow garbage collection
    path[stackSize] = null // allow garbage collection
    advanceIterator()
  }

  override fun beginObject() = apply {
    if (peek() != JsonReader.Token.BEGIN_OBJECT) {
      throw JsonDataException("Expected BEGIN_OBJECT but was ${peek()} at path ${getPath()}")
    }

    @Suppress("UNCHECKED_CAST")
    val currentValue = peekedData as Map<String, Any?>

    stackSize++
    path[stackSize - 1] = null
    iteratorStack[stackSize - 1] = currentValue.iterator()
    advanceIterator()
  }

  override fun endObject() = apply {
    if (peek() != JsonReader.Token.END_OBJECT) {
      throw JsonDataException("Expected END_OBJECT but was ${peek()} at path ${getPath()}")
    }
    stackSize--
    iteratorStack[stackSize] = null // allow garbage collection
    path[stackSize] = null // allow garbage collection
    advanceIterator()
  }

  override fun hasNext(): Boolean {
    return when (peek()) {
      JsonReader.Token.END_OBJECT -> false
      JsonReader.Token.END_ARRAY -> false
      else -> true
    }
  }

  override fun peek(): JsonReader.Token {
    return peekedToken
  }

  override fun nextName(): String {
    if (peek() != JsonReader.Token.NAME) {
      throw JsonDataException("Expected NAME but was ${peek()} at path ${getPath()}")
    }
    @Suppress("UNCHECKED_CAST")
    val data = peekedData as Map.Entry<String, Any?>

    path[stackSize - 1] = data.key
    peekedData = data.value
    peekedToken = anyToToken(data.value)
    return data.key
  }

  override fun nextString(): String? {
    // If we have a number, we convert it to a string
    when (peek()) {
      JsonReader.Token.STRING,
      JsonReader.Token.NUMBER,
      JsonReader.Token.LONG,
      -> Unit
      else -> {
        throw JsonDataException("Expected a String but was ${peek()} at path ${getPath()}")
      }
    }

    return (peekedData!!.toString()).also {
      advanceIterator()
    }
  }

  override fun nextBoolean(): Boolean {
    if (peek() != JsonReader.Token.BOOLEAN) {
      throw JsonDataException("Expected BOOLEAN but was ${peek()} at path ${getPath()}")
    }

    return (peekedData as Boolean).also {
      advanceIterator()
    }
  }

  override fun nextNull(): Nothing? {
    if (peek() != JsonReader.Token.NULL) {
      throw JsonDataException("Expected NULL but was ${peek()} at path ${getPath()}")
    }

    advanceIterator()

    return null
  }

  override fun nextDouble(): Double {
    when (peek()) {
      JsonReader.Token.STRING,
      JsonReader.Token.NUMBER,
      JsonReader.Token.LONG,
      -> Unit
      else -> {
        throw JsonDataException("Expected a Double but was ${peek()} at path ${getPath()}")
      }
    }

    return when (val value = peekedData) {
      is Int -> value.toDouble()
      is Long -> value.toDoubleExact()
      is Double -> value
      is String -> value.toDouble()
      is JsonNumber -> value.value.toDouble()
      else -> error("Expected a Double but got $value instead")
    }.also {
      advanceIterator()
    }
  }

  override fun nextInt(): Int {
    when (peek()) {
      JsonReader.Token.STRING,
      JsonReader.Token.NUMBER,
      JsonReader.Token.LONG,
      -> Unit
      else -> {
        throw JsonDataException("Expected an Int but was ${peek()} at path ${getPath()}")
      }
    }

    return when (val value = peekedData) {
      is Int -> value
      is Long -> value.toIntExact()
      is Double -> value.toIntExact()
      is String -> value.toInt()
      is JsonNumber -> value.value.toInt()
      else -> error("Expected an Int but got $value instead")
    }.also {
      advanceIterator()
    }
  }

  override fun nextLong(): Long {
    when (peek()) {
      JsonReader.Token.STRING,
      JsonReader.Token.NUMBER,
      JsonReader.Token.LONG,
      -> Unit
      else -> {
        throw JsonDataException("Expected a Long but was ${peek()} at path ${getPath()}")
      }
    }

    return when (val value = peekedData) {
      is Int -> value.toLong()
      is Long -> value
      is Double -> value.toLongExact()
      is String -> value.toLong()
      is JsonNumber -> value.value.toLong()
      else -> error("Expected Int but got $value instead")
    }.also {
      advanceIterator()
    }
  }

  override fun nextNumber(): JsonNumber {
    when (peek()) {
      JsonReader.Token.STRING,
      JsonReader.Token.NUMBER,
      JsonReader.Token.LONG,
      -> Unit
      else -> {
        throw JsonDataException("Expected a Number but was ${peek()} at path ${getPath()}")
      }
    }

    return when (val value = peekedData) {
      is Int, is Long, is Double -> JsonNumber(value.toString())
      is String -> JsonNumber(value) // assert value is a valid number
      is JsonNumber -> value
      else -> error("Expected JsonNumber but got $value instead")
    }.also {
      advanceIterator()
    }
  }

  override fun skipValue() {
    advanceIterator()
  }

  override fun close() {
  }

  override fun selectName(names: List<String>): Int {
    while (hasNext()) {
      val name = nextName()
      val index = names.indexOf(name)
      if (index != -1) {
        return index
      }

      // A name was present in the json but not in the expected list
      skipValue()
    }

    return -1
  }

  /**
   * Rewinds to the beginning of the current object.
   */
  override fun rewind() {
    TODO()
  }

  override fun getPath(): String {
    return buildString {
      path.forEachIndexed { index, element ->
        if (element is String) {
          if (index > 0) {
            append('.')
          }
          append(element)
        } else if (element is Int) {
          append("[$element]")
        } else {
          // unterminated object
          append('.')
        }
      }
    }

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
