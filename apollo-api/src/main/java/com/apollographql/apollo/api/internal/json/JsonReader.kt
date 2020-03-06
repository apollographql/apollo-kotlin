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

import java.io.Closeable
import java.io.IOException

/**
 * Reads a JSON (<a href="http://www.ietf.org/rfc/rfc7159.txt">RFC 7159</a>)
 * encoded value as a stream of tokens. This stream includes both literal
 * values (strings, numbers, booleans, and nulls) as well as the begin and
 * end delimiters of objects and arrays. The tokens are traversed in
 * depth-first order, the same order that they appear in the JSON document.
 * Within JSON objects, name/value pairs are represented by a single token.
 *
 * <p>Each {@code JsonReader} may be used to read a single JSON stream. Instances
 * of this class are not thread safe.
 */
interface JsonReader : Closeable {

  /**
   * Configure this parser to be liberal in what it accepts. By default
   * this parser is strict and only accepts JSON as specified by <a
   * href="http://www.ietf.org/rfc/rfc7159.txt">RFC 7159</a>.
   */
  var lenient: Boolean

  /**
   * Configure whether this parser throws a [JsonDataException] when [.skipValue] is
   * called. By default this parser permits values to be skipped.
   *
   * Forbid skipping to prevent unrecognized values from being silently ignored. This option is
   * useful in development and debugging because it means a typo like "locatiom" will be detected
   * early. It's potentially harmful in production because it complicates revising a JSON schema.
   */
  var failOnUnknown: Boolean

  /**
   * Consumes the next token from the JSON stream and asserts that it is the beginning of a new
   * array.
   */
  @Throws(IOException::class)
  fun beginArray()

  /**
   * Consumes the next token from the JSON stream and asserts that it is the
   * end of the current array.
   */
  @Throws(IOException::class)
  fun endArray()

  /**
   * Consumes the next token from the JSON stream and asserts that it is the beginning of a new
   * object.
   */
  @Throws(IOException::class)
  fun beginObject()

  /**
   * Consumes the next token from the JSON stream and asserts that it is the end of the current
   * object.
   */
  @Throws(IOException::class)
  fun endObject()

  /**
   * Returns true if the current array or object has another element.
   */
  @Throws(IOException::class)
  operator fun hasNext(): Boolean

  /**
   * Returns the type of the next token without consuming it.
   */
  @Throws(IOException::class)
  fun peek(): Token

  /**
   * Returns the next token, a [property name][Token.NAME], and consumes it.
   *
   * @throws JsonDataException if the next token in the stream is not a property name.
   */
  @Throws(IOException::class)
  fun nextName(): String

  /**
   * Returns the [string][Token.STRING] value of the next token, consuming it. If the next
   * token is a number, this method will return its string form.
   *
   * @throws JsonDataException if the next token is not a string or if this reader is closed.
   */
  @Throws(IOException::class)
  fun nextString(): String?

  /**
   * Returns the [boolean][Token.BOOLEAN] value of the next token, consuming it.
   *
   * @throws JsonDataException if the next token is not a boolean or if this reader is closed.
   */
  @Throws(IOException::class)
  fun nextBoolean(): Boolean

  /**
   * Consumes the next token from the JSON stream and asserts that it is a literal null. Returns
   * null.
   *
   * @throws JsonDataException if the next token is not null or if this reader is closed.
   */
  @Throws(IOException::class)
  fun <T> nextNull(): T?

  /**
   * Returns the [double][Token.NUMBER] value of the next token, consuming it. If the next
   * token is a string, this method will attempt to parse it as a double using [ ][Double.parseDouble].
   *
   * @throws JsonDataException if the next token is not a literal value, or if the next literal
   * value cannot be parsed as a double, or is non-finite.
   */
  @Throws(IOException::class)
  fun nextDouble(): Double

  /**
   * Returns the [long][Token.NUMBER] value of the next token, consuming it. If the next
   * token is a string, this method will attempt to parse it as a long. If the next token's numeric
   * value cannot be exactly represented by a Java `long`, this method throws.
   *
   * @throws JsonDataException if the next token is not a literal value, if the next literal value
   * cannot be parsed as a number, or exactly represented as a long.
   */
  @Throws(IOException::class)
  fun nextLong(): Long

  /**
   * Returns the [int][Token.NUMBER] value of the next token, consuming it. If the next
   * token is a string, this method will attempt to parse it as an int. If the next token's numeric
   * value cannot be exactly represented by a Java `int`, this method throws.
   *
   * @throws JsonDataException if the next token is not a literal value, if the next literal value
   * cannot be parsed as a number, or exactly represented as an int.
   */
  @Throws(IOException::class)
  fun nextInt(): Int

  /**
   * Skips the next value recursively. If it is an object or array, all nested elements are skipped.
   * This method is intended for use when the JSON token stream contains unrecognized or unhandled
   * values.
   *
   *
   * This throws a [JsonDataException] if this parser has been configured to [ ][.failOnUnknown] values.
   */
  @Throws(IOException::class)
  fun skipValue()

  /**
   * Returns a [JsonPath](http://goessner.net/articles/JsonPath/) to
   * the current location in the JSON value.
   */
  fun getPath(): String

  /**
   * Changes the reader to treat the next name as a string value. This is useful for map adapters so
   * that arbitrary type adapters can use [.nextString] to read a name value.
   */
  @Throws(IOException::class)
  fun promoteNameToValue()

  /**
   * A structure, name, or value type in a JSON-encoded string.
   */
  enum class Token {
    /**
     * The opening of a JSON array. Written using [JsonWriter.beginArray]
     * and read using [JsonReader.beginArray].
     */
    BEGIN_ARRAY,
    /**
     * The closing of a JSON array. Written using [JsonWriter.endArray]
     * and read using [JsonReader.endArray].
     */
    END_ARRAY,
    /**
     * The opening of a JSON object. Written using [JsonWriter.beginObject]
     * and read using [JsonReader.beginObject].
     */
    BEGIN_OBJECT,
    /**
     * The closing of a JSON object. Written using [JsonWriter.endObject]
     * and read using [JsonReader.endObject].
     */
    END_OBJECT,
    /**
     * A JSON property name. Within objects, tokens alternate between names and
     * their values. Written using [JsonWriter.name] and read using [ ][JsonReader.nextName]
     */
    NAME,
    /**
     * A JSON string.
     */
    STRING,
    /**
     * A JSON number represented in this API by a Java `double`, `long`, or `int`.
     */
    NUMBER,
    /**
     * A JSON `true` or `false`.
     */
    BOOLEAN,
    /**
     * A JSON `null`.
     */
    NULL,
    /**
     * The end of the JSON stream. This sentinel value is returned by [ ][JsonReader.peek] to signal that the JSON-encoded value has no more
     * tokens.
     */
    END_DOCUMENT
  }
}
