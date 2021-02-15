/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apollographql.apollo.api.internal.json

import com.apollographql.apollo.api.internal.Throws
import com.apollographql.apollo.api.internal.json.JsonScope.getPath
import okio.BufferedSink
import okio.IOException
import kotlin.jvm.JvmStatic

/**
 * Writes a JSON [RFC 7159](http://www.ietf.org/rfc/rfc7159.txt) encoded value to a stream, one token at a time.
 *
 * The stream includes both literal values (strings, numbers, booleans and nulls) as well as the begin and end delimiters of objects
 * and arrays.
 *
 * **Encoding JSON**
 *
 * To encode your data as JSON, create a new `JsonWriter`. Each JSON document must contain one top-level array or object.
 * Call methods on the writer as you walk the structure's contents, nesting arrays and objects as necessary:
 *
 * - To write `arrays`, first call [beginArray]. Write each of the array's elements with the appropriate [value]. Finally close the array
 * using [endArray].
 * - To write `objects`, first call [beginObject]. Write each of the object's properties by alternating calls to [name] with the property's
 * value. Write property values with the appropriate [value] method or by nesting other objects or arrays. Finally close the object
 * using [endObject].
 *
 * Each `JsonWriter` may be used to write a single JSON stream. Instances of this class are not thread safe. Calls that would result in a
 * malformed JSON string will fail with an [IllegalStateException].
 */
abstract class JsonWriter : Closeable, Flushable {
  // The nesting stack. Using a manual array rather than an ArrayList saves 20%. This stack permits  up to 32 levels of nesting including
  // the top-level document. Deeper nesting is prone to trigger StackOverflowErrors.
  protected var stackSize = 0
  protected val scopes = IntArray(32)
  protected val pathNames = arrayOfNulls<String>(32)
  protected val pathIndices = IntArray(32)

  /**
   * A string containing a full set of spaces for a single level of indentation, or null for no pretty printing.
   */
  var indent: String? = null

  /**
   * Configure this writer to relax its syntax rules.
   *
   * By default, this writer only emits well-formed JSON as specified by [RFC 7159](http://www.ietf.org/rfc/rfc7159.txt).
   */
  var isLenient = false

  /**
   * Sets whether object members are serialized when their value is null. This has no impact on array elements.
   *
   * The default is false.
   */
  var serializeNulls = false

  /**
   * Returns the scope on the top of the stack.
   */
  fun peekScope(): Int {
    check(stackSize != 0) { "JsonWriter is closed." }
    return scopes[stackSize - 1]
  }

  fun pushScope(newTop: Int) {
    if (stackSize == scopes.size) {
      throw JsonDataException("Nesting too deep at $path: circular reference?")
    }
    scopes[stackSize++] = newTop
  }

  /**
   * Replace the value on the top of the stack with the given value.
   */
  fun replaceTop(topOfStack: Int) {
    scopes[stackSize - 1] = topOfStack
  }

  /**
   * Begins encoding a new array. Each call to this method must be paired with a call to [endArray].
   */
  @Throws(IOException::class)
  abstract fun beginArray(): JsonWriter

  /**
   * Ends encoding the current array.
   */
  @Throws(IOException::class)
  abstract fun endArray(): JsonWriter

  /**
   * Begins encoding a new object. Each call to this method must be paired with a call to [endObject].
   */
  @Throws(IOException::class)
  abstract fun beginObject(): JsonWriter

  /**
   * Ends encoding the current object.
   */
  @Throws(IOException::class)
  abstract fun endObject(): JsonWriter

  /**
   * Encodes the property name.
   */
  @Throws(IOException::class)
  abstract fun name(name: String): JsonWriter

  /**
   * Encodes the literal string `value`, or null to encode a null literal.
   */
  @Throws(IOException::class)
  abstract fun value(value: String?): JsonWriter

  @Throws(IOException::class)
  abstract fun jsonValue(value: String?): JsonWriter

  /**
   * Encodes `null`.
   */
  @Throws(IOException::class)
  abstract fun nullValue(): JsonWriter

  /**
   * Encodes boolean `value`.
   */
  @Throws(IOException::class)
  abstract fun value(value: Boolean?): JsonWriter

  /**
   * Encodes a finite double `value`.
   *
   * May not be [Double.isNaN] or [Double.isInfinite].
   */
  @Throws(IOException::class)
  abstract fun value(value: Double): JsonWriter

  /**
   * Encodes long `value`.
   */
  @Throws(IOException::class)
  abstract fun value(value: Long): JsonWriter

  /**
   * Encodes number `value`.
   *
   * May not be [Double.isNaN] or [Double.isInfinite].
   */
  @Throws(IOException::class)
  abstract fun value(value: Number?): JsonWriter

  /**
   * Returns a [JsonPath](http://goessner.net/articles/JsonPath/) to the current location in the JSON value.
   */
  val path: String
    get() = getPath(stackSize, scopes, pathNames, pathIndices)

  companion object {
    /**
     * Returns a new instance that writes UTF-8 encoded JSON to `sink`.
     */
    @JvmStatic
    fun of(sink: BufferedSink): JsonWriter {
      return JsonUtf8Writer(sink)
    }
  }
}
