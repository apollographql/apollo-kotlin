package com.apollographql.apollo.api.json

import com.apollographql.apollo.api.json.BufferedSourceJsonReader.Companion.INITIAL_STACK_SIZE
import com.apollographql.apollo.api.json.MapJsonReader.Companion.buffer
import com.apollographql.apollo.api.json.internal.toDoubleExact
import com.apollographql.apollo.api.json.internal.toIntExact
import com.apollographql.apollo.api.json.internal.toLongExact
import com.apollographql.apollo.exception.JsonDataException
import kotlin.jvm.JvmOverloads

/**
 * A [JsonReader] that can consumes [ApolloJsonElement] values as Json
 *
 * To read from a [okio.BufferedSource], see also [BufferedSourceJsonReader]
 *
 * @param root the root object to read from
 * @param pathRoot the path root to be prefixed to the returned path when calling [getPath]. Useful for [buffer].
 */
class MapJsonReader
@JvmOverloads
constructor(
    val root: Any?,
    private val pathRoot: List<Any> = emptyList(),
) : JsonReader {

  private var peekedToken: JsonReader.Token

  /**
   * Depending on what [peekedToken] is, [peekedData] can be safely cast to a Map, Entry
   * or other values
   */
  private var peekedData: Any? = null


  /**
   * Can contain either:
   * - an Int representing the next index to be read in a List
   * - a String representing the current key to be read in a Map
   * - null if peekedToken is BEGIN_OBJECT
   */
  private var path = arrayOfNulls<Any>(INITIAL_STACK_SIZE)

  /**
   * The current object memorized in case we need to rewind
   */
  private var containerStack = arrayOfNulls<Map<String, Any?>>(INITIAL_STACK_SIZE)
  private var iteratorStack = arrayOfNulls<Iterator<*>>(INITIAL_STACK_SIZE)
  private var nameIndexStack = IntArray(INITIAL_STACK_SIZE)

  private var stackSize = 0

  init {
    peekedToken = anyToToken(root)
    peekedData = root
  }

  private fun anyToToken(any: Any?) = when (any) {
    null -> JsonReader.Token.NULL
    is List<*> -> JsonReader.Token.BEGIN_ARRAY
    is Map<*, *> -> JsonReader.Token.BEGIN_OBJECT
    is Int -> JsonReader.Token.NUMBER
    is Long -> JsonReader.Token.LONG
    is Double -> JsonReader.Token.NUMBER
    is JsonNumber -> JsonReader.Token.NUMBER
    is String -> JsonReader.Token.STRING
    is Boolean -> JsonReader.Token.BOOLEAN
    else -> JsonReader.Token.ANY
  }

  /**
   * Updates [peekedToken] and [peekedData]
   *
   * Requires [iteratorStack] and [path]
   */
  private fun advanceIterator() {
    if (stackSize == 0) {
      peekedToken = JsonReader.Token.END_DOCUMENT
      return
    }

    val currentIterator = iteratorStack[stackSize - 1]!!

    if (path[stackSize - 1] is Int) {
      path[stackSize - 1] = (path[stackSize - 1] as Int) + 1
    }

    if (currentIterator.hasNext()) {
      val next = currentIterator.next()
      peekedData = next

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

  private fun increaseStack() {
    if (stackSize == path.size) {
      path = path.copyOf(path.size * 2)
      containerStack = containerStack.copyOf(containerStack.size * 2)
      nameIndexStack = nameIndexStack.copyOf(nameIndexStack.size * 2)
      iteratorStack = iteratorStack.copyOf(iteratorStack.size * 2)
    }
    stackSize++
  }

  override fun beginArray() = apply {
    if (peek() != JsonReader.Token.BEGIN_ARRAY) {
      throw JsonDataException("Expected BEGIN_ARRAY but was ${peek()} at path ${getPathAsString()}")
    }

    val currentValue = peekedData as List<Any?>

    increaseStack()

    path[stackSize - 1] = -1
    iteratorStack[stackSize - 1] = currentValue.iterator()
    advanceIterator()
  }

  override fun endArray() = apply {
    if (peek() != JsonReader.Token.END_ARRAY) {
      throw JsonDataException("Expected END_ARRAY but was ${peek()} at path ${getPathAsString()}")
    }
    stackSize--
    iteratorStack[stackSize] = null // allow garbage collection
    path[stackSize] = null // allow garbage collection
    advanceIterator()
  }

  override fun beginObject() = apply {
    if (peek() != JsonReader.Token.BEGIN_OBJECT) {
      throw JsonDataException("Expected BEGIN_OBJECT but was ${peek()} at path ${getPathAsString()}")
    }

    increaseStack()

    @Suppress("UNCHECKED_CAST")
    containerStack[stackSize - 1] = peekedData as Map<String, Any?>

    rewind()
  }

  override fun endObject() = apply {
    // Do not fail if there are trailing names in buffered readers.
    // See https://github.com/apollographql/apollo-kotlin/issues/4212
//    if (peek() != JsonReader.Token.END_OBJECT) {
//      throw JsonDataException("Expected END_OBJECT but was ${peek()} at path ${getPathAsString()}")
//    }
    stackSize--
    iteratorStack[stackSize] = null // allow garbage collection
    path[stackSize] = null // allow garbage collection
    containerStack[stackSize] = null // allow garbage collection
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
      throw JsonDataException("Expected NAME but was ${peek()} at path ${getPathAsString()}")
    }
    @Suppress("UNCHECKED_CAST")
    val data = peekedData as Map.Entry<String, Any?>

    path[stackSize - 1] = data.key
    peekedData = data.value
    peekedToken = anyToToken(data.value)
    return data.key
  }

  override fun nextString(): String {
    return when (val value = peekedData) {
      is Int -> value.toString()
      is Long -> value.toString()
      is Double -> value.toString()
      is String -> value
      null -> "null"
      is JsonNumber -> value.value
      else -> error("Expected a String but got $value instead")
    }.also {
      advanceIterator()
    }
  }

  override fun nextBoolean(): Boolean {
    if (peek() != JsonReader.Token.BOOLEAN) {
      throw JsonDataException("Expected BOOLEAN but was ${peek()} at path ${getPathAsString()}")
    }

    return (peekedData as Boolean).also {
      advanceIterator()
    }
  }

  override fun nextNull(): Nothing? {
    if (peek() != JsonReader.Token.NULL) {
      throw JsonDataException("Expected NULL but was ${peek()} at path ${getPathAsString()}")
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
        throw JsonDataException("Expected a Double but was ${peek()} at path ${getPathAsString()}")
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
        throw JsonDataException("Expected an Int but was ${peek()} at path ${getPathAsString()}")
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
        throw JsonDataException("Expected a Long but was ${peek()} at path ${getPathAsString()}")
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
        throw JsonDataException("Expected a Number but was ${peek()} at path ${getPathAsString()}")
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

  fun nextValue(): Any {
    val data = peekedData ?: throw JsonDataException("Expected a non-null value at path ${getPathAsString()}")

    return data.also {
      advanceIterator()
    }
  }

  override fun skipValue() {
    advanceIterator()
  }

  override fun close() {
  }

  private fun findName(needle: String, haystack: List<String>): Int {
    val expectedIndex = nameIndexStack[stackSize - 1]
    if (expectedIndex < haystack.size && haystack[expectedIndex] == needle) {
      /**
       * Our guess succeeded
       *
       * Note that for a same object, haystack might have different size.
       * For an example, for responseBased codegen and polymorphic fields, this is going
       * to be called once with just ["__typename"] and then later on with ["__typename", "id", "name"]
       */
      nameIndexStack[stackSize - 1] = nameIndexStack[stackSize - 1] + 1
      return expectedIndex
    } else {
      // guess failed, go back to full search
      val index = haystack.indexOf(needle)
      if (index != -1) {
        // reset the prediction
        nameIndexStack[stackSize - 1] = index + 1
      }
      return index
    }
  }

  override fun selectName(names: List<String>): Int {
    while (hasNext()) {
      val name = nextName()
      val index = findName(name, names)
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
    val container = containerStack[stackSize - 1]
    path[stackSize - 1] = null
    iteratorStack[stackSize - 1] = container!!.iterator()
    nameIndexStack[stackSize - 1] = 0
    advanceIterator()
  }

  override fun getPath(): List<Any> {
    val result = mutableListOf<Any>()
    result.addAll(pathRoot)
    for (index in 0 until stackSize) {
      path[index]?.let { result += it }
    }
    return result
  }

  private fun getPathAsString() = getPath().joinToString(".")

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

      val pathRoot = getPath()

      @Suppress("UNCHECKED_CAST")
      val data = this.readAny() as Map<String, Any?>
      return MapJsonReader(root = data, pathRoot = pathRoot)
    }
  }
}

/**
 * A typealias for a type-unsafe Kotlin representation of JSON. This typealias is
 * mainly for internal documentation purposes and low-level manipulations and should
 * generally be avoided in application code.
 *
 * [ApolloJsonElement] can be any of:
 * - null
 * - String
 * - Boolean
 * - Int
 * - Long
 * - Double
 * - JsonNumber
 * - Map<String, ApolloJsonElement> where values are any of these values recursively
 * - List<ApolloJsonElement> where values are any of these values recursively
 * - dynamic for advanced use cases using @JsExport
 *
 * Anything else is undefined
 */
typealias ApolloJsonElement = Any?
