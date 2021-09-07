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
package com.apollographql.apollo3.api.json

import com.apollographql.apollo3.api.Throws
import com.apollographql.apollo3.api.Upload
import okio.IOException

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
 *
 * **Note**: This interface was originally from Moshi and has been tweaked to better match the GraphQL use cases such as the
 * absence of arbitrary precision and the presence of the [Upload]
 */
interface JsonWriter : Closeable {
  /**
   * Begins encoding a new array. Each call to this method must be paired with a call to [endArray].
   */
  fun beginArray(): JsonWriter

  /**
   * Ends encoding the current array.
   */
  fun endArray(): JsonWriter

  /**
   * Begins encoding a new object. Each call to this method must be paired with a call to [endObject].
   */
  fun beginObject(): JsonWriter

  /**
   * Ends encoding the current object.
   */
  fun endObject(): JsonWriter

  /**
   * Encodes the property name.
   */
  fun name(name: String): JsonWriter

  /**
   * Encodes the literal string `value`, or null to encode a null literal.
   */
  fun value(value: String): JsonWriter
  
  /**
   * Encodes `null`.
   */
  fun nullValue(): JsonWriter

  /**
   * Encodes boolean `value`.
   */
  fun value(value: Boolean): JsonWriter

  /**
   * Encodes a finite double `value`.
   *
   * May not be [Double.isNaN] or [Double.isInfinite].
   */
  fun value(value: Double): JsonWriter

  /**
   * Encodes int `value`.
   */
  fun value(value: Int): JsonWriter

  /**
   * Encodes a [Upload].
   */
  fun value(value: Upload): JsonWriter

  val path: String

  /**
   * Flushes the writer
   */
  fun flush()
}
