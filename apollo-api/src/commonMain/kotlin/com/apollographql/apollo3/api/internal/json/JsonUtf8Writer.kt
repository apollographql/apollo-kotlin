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
import okio.BufferedSink
import okio.ByteString
import okio.IOException

internal class JsonUtf8Writer(private val sink: BufferedSink) : JsonWriter() {
  companion object {
    private const val HEX_ARRAY = "0123456789abcdef"

    private fun Byte.hexString(): String {
      val value = toInt()
      return "${HEX_ARRAY[value.ushr(4)]}${HEX_ARRAY[value and 0x0F]}"
    }

    /**
     * From RFC 7159, "All Unicode characters may be placed within the quotation marks except for the characters that must be escaped:
     * quotation mark, reverse solidus, and the control characters (U+0000 through U+001F)."
     *
     * We also escape '\u2028' and '\u2029', which JavaScript interprets as newline characters. This prevents eval() from failing with a
     * syntax error. http://code.google.com/p/google-gson/issues/detail?id=341
     */
    private val REPLACEMENT_CHARS: Array<String?> = arrayOfNulls<String?>(128).apply {
      for (i in 0..0x1f) {
        this[i] = "\\u00${i.toByte().hexString()}"
      }
      this['"'.toInt()] = "\\\""
      this['\\'.toInt()] = "\\\\"
      this['\t'.toInt()] = "\\t"
      this['\b'.toInt()] = "\\b"
      this['\n'.toInt()] = "\\n"
      this['\r'.toInt()] = "\\r"
    }

    /**
     * Writes `value` as a string literal to `sink`. This wraps the value in double quotes and escapes those characters that require it.
     */
    @Throws(IOException::class)
    fun string(sink: BufferedSink, value: String) {
      val replacements = REPLACEMENT_CHARS
      sink.writeByte('"'.toInt())
      var last = 0
      val length = value.length
      for (i in 0 until length) {
        val c = value[i]
        var replacement: String?
        if (c.toInt() < 128) {
          replacement = replacements[c.toInt()]
          if (replacement == null) {
            continue
          }
        } else if (c == '\u2028') {
          replacement = "\\u2028"
        } else if (c == '\u2029') {
          replacement = "\\u2029"
        } else {
          continue
        }
        if (last < i) {
          sink.writeUtf8(value, last, i)
        }
        sink.writeUtf8(replacement)
        last = i + 1
      }
      if (last < length) {
        sink.writeUtf8(value, last, length)
      }
      sink.writeByte('"'.toInt())
    }
  }

  /** The name/value separator; either ":" or ": ".  */
  val separator: String
    get() = if (indent.isNullOrEmpty()) ":" else ": "

  private var deferredName: String? = null

  @Throws(IOException::class)
  override fun beginArray(): JsonWriter {
    writeDeferredName()
    return open(JsonScope.EMPTY_ARRAY, "[")
  }

  @Throws(IOException::class)
  override fun endArray(): JsonWriter {
    return close(JsonScope.EMPTY_ARRAY, JsonScope.NONEMPTY_ARRAY, "]")
  }

  @Throws(IOException::class)
  override fun beginObject(): JsonWriter {
    writeDeferredName()
    return open(JsonScope.EMPTY_OBJECT, "{")
  }

  @Throws(IOException::class)
  override fun endObject(): JsonWriter {
    return close(JsonScope.EMPTY_OBJECT, JsonScope.NONEMPTY_OBJECT, "}")
  }

  /**
   * Enters a new scope by appending any necessary whitespace and the given bracket.
   */
  @Throws(IOException::class)
  private fun open(empty: Int, openBracket: String): JsonWriter {
    beforeValue()
    pushScope(empty)
    pathIndices[stackSize - 1] = 0
    sink.writeUtf8(openBracket)
    return this
  }

  /**
   * Closes the current scope by appending any necessary whitespace and the given bracket.
   */
  @Throws(IOException::class)
  private fun close(empty: Int, nonempty: Int, closeBracket: String): JsonWriter {
    val context = peekScope()
    check(!(context != nonempty && context != empty)) { "Nesting problem." }
    check(deferredName == null) { "Dangling name: $deferredName" }
    stackSize--
    pathNames[stackSize] = null // Free the last path name so that it can be garbage collected!
    pathIndices[stackSize - 1]++
    if (context == nonempty) {
      newline()
    }
    sink.writeUtf8(closeBracket)
    return this
  }

  @Throws(IOException::class)
  override fun name(name: String): JsonWriter {
    check(stackSize != 0) { "JsonWriter is closed." }
    check(deferredName == null) { "Nesting problem." }
    deferredName = name
    pathNames[stackSize - 1] = name
    return this
  }

  @Throws(IOException::class)
  private fun writeDeferredName() {
    if (deferredName != null) {
      beforeName()
      string(sink, deferredName!!)
      deferredName = null
    }
  }

  @Throws(IOException::class)
  override fun value(value: String?): JsonWriter {
    if (value == null) {
      return nullValue()
    }
    writeDeferredName()
    beforeValue()
    string(sink, value)
    pathIndices[stackSize - 1]++
    return this
  }

  @Throws(IOException::class)
  override fun jsonValue(value: String?): JsonWriter {
    if (value == null) {
      return nullValue()
    }
    writeDeferredName()
    beforeValue()
    sink.writeUtf8(value)
    pathIndices[stackSize - 1]++
    return this
  }

  @Throws(IOException::class)
  override fun nullValue(): JsonWriter {
    if (deferredName != null) {
      if (serializeNulls) {
        writeDeferredName()
      } else {
        deferredName = null
        return this // skip the name and the value
      }
    }
    beforeValue()
    sink.writeUtf8("null")
    pathIndices[stackSize - 1]++
    return this
  }

  @Throws(IOException::class)
  override fun value(value: Boolean?): JsonWriter {
    return if (value == null) {
      nullValue()
    } else {
      writeDeferredName()
      beforeValue()
      sink.writeUtf8(if (value) "true" else "false")
      pathIndices[stackSize - 1]++
      this
    }
  }

  @Throws(IOException::class)
  override fun value(value: Double): JsonWriter {
    require(!(!isLenient && (value.isNaN() || value.isInfinite()))) {
      "Numeric values must be finite, but was $value"
    }
    writeDeferredName()
    beforeValue()
    sink.writeUtf8(value.toString())
    pathIndices[stackSize - 1]++
    return this
  }

  @Throws(IOException::class)
  override fun value(value: Long): JsonWriter {
    writeDeferredName()
    beforeValue()
    sink.writeUtf8(value.toString())
    pathIndices[stackSize - 1]++
    return this
  }

  @Throws(IOException::class)
  override fun value(value: Number?): JsonWriter {
    if (value == null) {
      return nullValue()
    }
    val string = value.toString()
    require(!(!isLenient && (string == "-Infinity" || string == "Infinity" || string == "NaN"))) {
      "Numeric values must be finite, but was $value"
    }
    writeDeferredName()
    beforeValue()
    sink.writeUtf8(string)
    pathIndices[stackSize - 1]++
    return this
  }

  /**
   * Ensures all buffered data is written to the underlying [okio.Sink]
   * and flushes that writer.
   */
  @Throws(IOException::class)
  override fun flush() {
    check(stackSize != 0) { "JsonWriter is closed." }
    sink.flush()
  }

  /**
   * Flushes and closes this writer and the underlying [okio.Sink].
   *
   * @throws IOException if the JSON document is incomplete.
   */
  @Throws(IOException::class)
  override fun close() {
    sink.close()
    val size = stackSize
    if (size > 1 || size == 1 && scopes[size - 1] != JsonScope.NONEMPTY_DOCUMENT) {
      throw IOException("Incomplete document")
    }
    stackSize = 0
  }

  @Throws(IOException::class)
  private fun newline() {
    if (indent == null) {
      return
    }
    sink.writeByte('\n'.toInt())
    var i = 1
    val size = stackSize
    while (i < size) {
      sink.writeUtf8(indent ?: "")
      i++
    }
  }

  /**
   * Inserts any necessary separators and whitespace before a name. Also adjusts the stack to expect the name's value.
   */
  @Throws(IOException::class)
  private fun beforeName() {
    val context = peekScope()
    if (context == JsonScope.NONEMPTY_OBJECT) { // first in object
      sink.writeByte(','.toInt())
    } else check(context == JsonScope.EMPTY_OBJECT) {
      // not in an object!
      "Nesting problem."
    }
    newline()
    replaceTop(JsonScope.DANGLING_NAME)
  }

  /**
   * Inserts any necessary separators and whitespace before a literal value, inline array, or inline object. Also adjusts the stack to
   * expect either a closing bracket or another element.
   */
  @Throws(IOException::class)
  private fun beforeValue() {
    when (peekScope()) {
      JsonScope.NONEMPTY_DOCUMENT -> {
        check(isLenient) { "JSON must have only one top-level value." }
        replaceTop(JsonScope.NONEMPTY_DOCUMENT)
      }
      JsonScope.EMPTY_DOCUMENT -> replaceTop(JsonScope.NONEMPTY_DOCUMENT)
      JsonScope.EMPTY_ARRAY -> {
        replaceTop(JsonScope.NONEMPTY_ARRAY)
        newline()
      }
      JsonScope.NONEMPTY_ARRAY -> {
        sink.writeByte(','.toInt())
        newline()
      }
      JsonScope.DANGLING_NAME -> {
        sink.writeUtf8(separator)
        replaceTop(JsonScope.NONEMPTY_OBJECT)
      }
      else -> throw IllegalStateException("Nesting problem.")
    }
  }

  init {
    pushScope(JsonScope.EMPTY_DOCUMENT)
  }
}
