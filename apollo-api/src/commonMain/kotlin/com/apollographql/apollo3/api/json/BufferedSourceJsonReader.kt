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

import com.apollographql.apollo3.api.json.internal.JsonScope
import com.apollographql.apollo3.exception.JsonDataException
import com.apollographql.apollo3.exception.JsonEncodingException
import okio.Buffer
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.EOFException
import okio.IOException

/**
 * A [JsonWriter] that reads json from an okio [BufferedSource]
 *
 * The base implementation was taken from Moshi and ported to Kotlin multiplatform with some tweaks to make it better suited for GraphQL
 * (see [JsonReader]).
 *
 * To read from a [Map], see also [MapJsonReader]
 */
class BufferedSourceJsonReader(private val source: BufferedSource) : JsonReader {
  private val buffer: Buffer = source.buffer
  private var peeked = PEEKED_NONE

  /**
   * A peeked value that was composed entirely of digits with an optional leading dash. Positive values may not have a leading 0.
   */
  private var peekedLong: Long = 0

  /**
   * The number of characters in a peeked number literal. Increment 'pos' by this after reading a number.
   */
  private var peekedNumberLength = 0

  /**
   * A peeked string that should be parsed on the next double, long or string.
   * This is populated before a numeric value is parsed and used if that parsing fails.
   */
  private var peekedString: String? = null

  /**
   * The nesting stack. Using a manual array rather than an ArrayList saves 20%.
   * This stack permits up to MAX_STACK_SIZE levels of nesting including the top-level document.
   * Deeper nesting is prone to trigger StackOverflowErrors.
   */
  private val stack = IntArray(MAX_STACK_SIZE).apply {
    this[0] = JsonScope.EMPTY_DOCUMENT
  }
  private var stackSize = 1
  private val pathNames = arrayOfNulls<String>(MAX_STACK_SIZE)
  private val pathIndices = IntArray(MAX_STACK_SIZE)

  private var lenient = false

  private val indexStack = IntArray(MAX_STACK_SIZE).apply {
    this[0] = 0
  }
  private var indexStackSize = 1

  override fun beginArray(): JsonReader = apply {
    val p = peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()
    if (p == PEEKED_BEGIN_ARRAY) {
      push(JsonScope.EMPTY_ARRAY)
      pathIndices[stackSize - 1] = 0
      peeked = PEEKED_NONE
    } else {
      throw JsonDataException("Expected BEGIN_ARRAY but was ${peek()} at path ${getPath()}")
    }
  }

  override fun endArray(): JsonReader = apply {
    val p = peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()
    if (p == PEEKED_END_ARRAY) {
      stackSize--
      pathIndices[stackSize - 1]++
      peeked = PEEKED_NONE
    } else {
      throw JsonDataException("Expected END_ARRAY but was ${peek()} at path ${getPath()}")
    }
  }

  override fun beginObject(): JsonReader = apply {
    val p = peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()
    if (p == PEEKED_BEGIN_OBJECT) {
      push(JsonScope.EMPTY_OBJECT)
      peeked = PEEKED_NONE

      indexStackSize++
      indexStack[indexStackSize - 1] = 0
    } else {
      throw JsonDataException("Expected BEGIN_OBJECT but was ${peek()} at path ${getPath()}")
    }
  }

  override fun endObject(): JsonReader = apply {
    val p = peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()
    if (p == PEEKED_END_OBJECT) {
      stackSize--
      pathNames[stackSize] = null // Free the last path name so that it can be garbage collected!
      pathIndices[stackSize - 1]++
      peeked = PEEKED_NONE

      indexStackSize--
    } else {
      throw JsonDataException("Expected END_OBJECT but was ${peek()} at path ${getPath()}")
    }
  }

  override fun hasNext(): Boolean {
    val p = peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()
    return p != PEEKED_END_OBJECT && p != PEEKED_END_ARRAY
  }

  override fun peek(): JsonReader.Token {
    return when (peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()) {
      PEEKED_BEGIN_OBJECT -> JsonReader.Token.BEGIN_OBJECT
      PEEKED_END_OBJECT -> JsonReader.Token.END_OBJECT
      PEEKED_BEGIN_ARRAY -> JsonReader.Token.BEGIN_ARRAY
      PEEKED_END_ARRAY -> JsonReader.Token.END_ARRAY
      PEEKED_SINGLE_QUOTED_NAME, PEEKED_DOUBLE_QUOTED_NAME, PEEKED_UNQUOTED_NAME -> JsonReader.Token.NAME
      PEEKED_TRUE, PEEKED_FALSE -> JsonReader.Token.BOOLEAN
      PEEKED_NULL -> JsonReader.Token.NULL
      PEEKED_SINGLE_QUOTED, PEEKED_DOUBLE_QUOTED, PEEKED_UNQUOTED, PEEKED_BUFFERED -> JsonReader.Token.STRING
      PEEKED_LONG -> JsonReader.Token.LONG
      PEEKED_NUMBER -> JsonReader.Token.NUMBER
      PEEKED_EOF -> JsonReader.Token.END_DOCUMENT
      else -> throw AssertionError()
    }
  }

  private fun doPeek(): Int {
    val peekStack = stack[stackSize - 1]

    when (peekStack) {
      JsonScope.EMPTY_ARRAY -> {
        stack[stackSize - 1] = JsonScope.NONEMPTY_ARRAY
      }

      JsonScope.NONEMPTY_ARRAY -> {
        // Look for a comma before the next element.
        val c = nextNonWhitespace(true)
        buffer.readByte() // consume ']' or ','.
        when (c.toChar()) {
          ']' -> return PEEKED_END_ARRAY.also { peeked = it }
          ';' -> checkLenient() // fall-through
          ',' -> Unit
          else -> throw syntaxError("Unterminated array")
        }
      }

      JsonScope.EMPTY_OBJECT, JsonScope.NONEMPTY_OBJECT -> {
        stack[stackSize - 1] = JsonScope.DANGLING_NAME
        // Look for a comma before the next element.
        if (peekStack == JsonScope.NONEMPTY_OBJECT) {
          val c = nextNonWhitespace(true)
          buffer.readByte() // Consume '}' or ','.
          when (c.toChar()) {
            '}' -> return PEEKED_END_OBJECT.also { peeked = it }
            ';' -> checkLenient() // fall-through
            ',' -> Unit
            else -> throw syntaxError("Unterminated object")
          }
        }

        val c = nextNonWhitespace(true)
        return when (c.toChar()) {
          '"' -> {
            buffer.readByte() // consume the '\"'.
            PEEKED_DOUBLE_QUOTED_NAME.also { peeked = it }
          }
          '\'' -> {
            buffer.readByte() // consume the '\''.
            checkLenient()
            PEEKED_SINGLE_QUOTED_NAME.also { peeked = it }
          }
          '}' -> if (peekStack != JsonScope.NONEMPTY_OBJECT) {
            buffer.readByte() // consume the '}'.
            PEEKED_END_OBJECT.also { peeked = it }
          } else {
            throw syntaxError("Expected name")
          }
          else -> {
            checkLenient()
            if (isLiteral(c.toChar())) {
              PEEKED_UNQUOTED_NAME.also { peeked = it }
            } else {
              throw syntaxError("Expected name")
            }
          }
        }
      }

      JsonScope.DANGLING_NAME -> {
        stack[stackSize - 1] = JsonScope.NONEMPTY_OBJECT
        // Look for a colon before the value.
        val c = nextNonWhitespace(true)
        buffer.readByte() // Consume ':'.
        when (c.toChar()) {
          ':' -> {
          }
          '=' -> {
            checkLenient()
            if (source.request(1) && buffer[0] == '>'.code.toByte()) {
              buffer.readByte() // Consume '>'.
            }
          }
          else -> throw syntaxError("Expected ':'")
        }
      }

      JsonScope.EMPTY_DOCUMENT -> {
        stack[stackSize - 1] = JsonScope.NONEMPTY_DOCUMENT
      }

      JsonScope.NONEMPTY_DOCUMENT -> {
        val c = nextNonWhitespace(false)
        if (c == -1) {
          return PEEKED_EOF.also { peeked = it }
        } else {
          checkLenient()
        }
      }

      else -> check(peekStack != JsonScope.CLOSED) { "JsonReader is closed" }
    }

    val c = nextNonWhitespace(true)
    when (c.toChar()) {
      ']' -> {
        if (peekStack == JsonScope.EMPTY_ARRAY) {
          buffer.readByte() // Consume ']'.
          return PEEKED_END_ARRAY.also { peeked = it }
        }
        // In lenient mode, a 0-length literal in an array means 'null'.
        return if (peekStack == JsonScope.EMPTY_ARRAY || peekStack == JsonScope.NONEMPTY_ARRAY) {
          checkLenient()
          PEEKED_NULL.also { peeked = it }
        } else {
          throw syntaxError("Unexpected value")
        }
      }
      ';', ',' -> return if (peekStack == JsonScope.EMPTY_ARRAY || peekStack == JsonScope.NONEMPTY_ARRAY) {
        checkLenient()
        PEEKED_NULL.also { peeked = it }
      } else {
        throw syntaxError("Unexpected value")
      }
      '\'' -> {
        checkLenient()
        buffer.readByte() // Consume '\''.
        return PEEKED_SINGLE_QUOTED.also { peeked = it }
      }
      '"' -> {
        buffer.readByte() // Consume '\"'.
        return PEEKED_DOUBLE_QUOTED.also { peeked = it }
      }
      '[' -> {
        buffer.readByte() // Consume '['.
        return PEEKED_BEGIN_ARRAY.also { peeked = it }
      }
      '{' -> {
        buffer.readByte() // Consume '{'.
        return PEEKED_BEGIN_OBJECT.also { peeked = it }
      }
    }

    var result = peekKeyword()
    if (result != PEEKED_NONE) {
      return result
    }

    result = peekNumber()
    if (result != PEEKED_NONE) {
      return result
    }

    if (!isLiteral(buffer[0].toInt().toChar())) {
      throw syntaxError("Expected value")
    }

    checkLenient()
    return PEEKED_UNQUOTED.also { peeked = it }
  }

  private fun peekKeyword(): Int { // Figure out which keyword we're matching against by its first character.
    val keyword: String
    val keywordUpper: String
    val peeking: Int
    when (buffer[0]) {
      't'.code.toByte(), 'T'.code.toByte() -> {
        keyword = "true"
        keywordUpper = "TRUE"
        peeking = PEEKED_TRUE
      }
      'f'.code.toByte(), 'F'.code.toByte() -> {
        keyword = "false"
        keywordUpper = "FALSE"
        peeking = PEEKED_FALSE
      }
      'n'.code.toByte(), 'N'.code.toByte() -> {
        keyword = "null"
        keywordUpper = "NULL"
        peeking = PEEKED_NULL
      }

      else -> return PEEKED_NONE
    }

    // Confirm that chars [1..length) match the keyword.
    val length = keyword.length
    for (i in 1 until length) {
      if (!source.request(i + 1.toLong())) {
        return PEEKED_NONE
      }
      val c = buffer[i.toLong()]
      if (c != keyword[i].code.toByte() && c != keywordUpper[i].code.toByte()) {
        return PEEKED_NONE
      }
    }

    if (source.request(length + 1.toLong()) && isLiteral(buffer[length.toLong()].toInt().toChar())) {
      return PEEKED_NONE // Don't match trues, falsey or nullsoft!
    }

    // We've found the keyword followed either by EOF or by a non-literal character.
    buffer.skip(length.toLong())
    return peeking.also { peeked = it }
  }

  private fun peekNumber(): Int {
    var value: Long = 0 // Negative to accommodate Long.MIN_VALUE more easily.
    var negative = false
    var fitsInLong = true
    var last = NUMBER_CHAR_NONE
    var i = 0
    loop@ while (source.request(i + 1.toLong())) {
      val c = buffer[i.toLong()]
      when (c.toInt().toChar()) {
        '-' -> {
          when (last) {
            NUMBER_CHAR_NONE -> {
              negative = true
              last = NUMBER_CHAR_SIGN
            }
            NUMBER_CHAR_EXP_E -> {
              last = NUMBER_CHAR_EXP_SIGN
            }
            else -> {
              return PEEKED_NONE
            }
          }
        }
        '+' -> {
          if (last == NUMBER_CHAR_EXP_E) {
            last = NUMBER_CHAR_EXP_SIGN
          } else {
            return PEEKED_NONE
          }
        }
        'e', 'E' -> {
          if (last == NUMBER_CHAR_DIGIT || last == NUMBER_CHAR_FRACTION_DIGIT) {
            last = NUMBER_CHAR_EXP_E
          } else {
            return PEEKED_NONE
          }
        }
        '.' -> {
          if (last == NUMBER_CHAR_DIGIT) {
            last = NUMBER_CHAR_DECIMAL
          } else {
            return PEEKED_NONE
          }
        }
        else -> {
          if (c < '0'.code.toByte() || c > '9'.code.toByte()) {
            if (!isLiteral(c.toInt().toChar())) {
              break@loop
            } else {
              return PEEKED_NONE
            }
          }

          when (last) {
            NUMBER_CHAR_SIGN, NUMBER_CHAR_NONE -> {
              value = -(c - '0'.code.toByte()).toLong()
              last = NUMBER_CHAR_DIGIT
            }

            NUMBER_CHAR_DIGIT -> {
              if (value == 0L) {
                return PEEKED_NONE // Leading '0' prefix is not allowed (since it could be octal).
              }
              val newValue = value * 10 - (c - '0'.code.toByte())
              fitsInLong = fitsInLong and (value > MIN_INCOMPLETE_INTEGER) || value == MIN_INCOMPLETE_INTEGER && newValue < value
              value = newValue
            }

            NUMBER_CHAR_DECIMAL -> {
              last = NUMBER_CHAR_FRACTION_DIGIT
            }

            NUMBER_CHAR_EXP_E, NUMBER_CHAR_EXP_SIGN -> {
              last = NUMBER_CHAR_EXP_DIGIT
            }
          }
        }
      }
      i++
    }

    // We've read a complete number. Decide if it's a PEEKED_LONG or a PEEKED_NUMBER.
    return if (last == NUMBER_CHAR_DIGIT && fitsInLong && (value != Long.MIN_VALUE || negative)) {
      peekedLong = if (negative) value else -value
      buffer.skip(i.toLong())
      PEEKED_LONG.also { peeked = it }
    } else if (last == NUMBER_CHAR_DIGIT || last == NUMBER_CHAR_FRACTION_DIGIT || last == NUMBER_CHAR_EXP_DIGIT) {
      peekedNumberLength = i
      PEEKED_NUMBER.also { peeked = it }
    } else {
      PEEKED_NONE
    }
  }

  private fun isLiteral(c: Char): Boolean {
    return when (c) {
      '/', '\\', ';', '#', '=' -> {
        checkLenient() // fall-through
        false
      }
      '{', '}', '[', ']', ':', ',', ' ', '\t', '\r', '\n' -> false
      else -> true
    }
  }

  override fun nextName(): String {
    val result = when (peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()) {
      PEEKED_UNQUOTED_NAME -> nextUnquotedValue()
      PEEKED_DOUBLE_QUOTED_NAME -> nextQuotedValue(DOUBLE_QUOTE_OR_SLASH)
      PEEKED_SINGLE_QUOTED_NAME -> nextQuotedValue(SINGLE_QUOTE_OR_SLASH)
      else -> throw JsonDataException("Expected a name but was ${peek()} at path ${getPath()}")
    }
    peeked = PEEKED_NONE
    pathNames[stackSize - 1] = result
    return result
  }

  override fun nextString(): String? {
    val result = when (peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()) {
      PEEKED_UNQUOTED -> nextUnquotedValue()
      PEEKED_DOUBLE_QUOTED -> nextQuotedValue(DOUBLE_QUOTE_OR_SLASH)
      PEEKED_SINGLE_QUOTED -> nextQuotedValue(SINGLE_QUOTE_OR_SLASH)
      PEEKED_BUFFERED -> peekedString?.also { peekedString = null }
      PEEKED_LONG -> peekedLong.toString()
      PEEKED_NUMBER -> buffer.readUtf8(peekedNumberLength.toLong())
      else -> throw JsonDataException("Expected a string but was ${peek()} at path ${getPath()}")
    }
    peeked = PEEKED_NONE
    pathIndices[stackSize - 1]++
    return result
  }

  override fun nextBoolean(): Boolean {
    return when (peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()) {
      PEEKED_TRUE -> {
        peeked = PEEKED_NONE
        pathIndices[stackSize - 1]++
        true
      }

      PEEKED_FALSE -> {
        peeked = PEEKED_NONE
        pathIndices[stackSize - 1]++
        false
      }

      else -> throw JsonDataException("Expected a boolean but was ${peek()} at path ${getPath()}")
    }
  }

  override fun nextNull(): Nothing? {
    return when (peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()) {
      PEEKED_NULL -> {
        peeked = PEEKED_NONE
        pathIndices[stackSize - 1]++
        null
      }

      else -> throw JsonDataException("Expected null but was ${peek()} at path ${getPath()}")
    }
  }

  override fun nextDouble(): Double {
    val p = peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()
    when {
      p == PEEKED_LONG -> {
        peeked = PEEKED_NONE
        pathIndices[stackSize - 1]++
        return peekedLong.toDouble()
      }
      p == PEEKED_NUMBER -> {
        peekedString = buffer.readUtf8(peekedNumberLength.toLong())
      }
      p == PEEKED_DOUBLE_QUOTED -> {
        peekedString = nextQuotedValue(DOUBLE_QUOTE_OR_SLASH)
      }
      p == PEEKED_SINGLE_QUOTED -> {
        peekedString = nextQuotedValue(SINGLE_QUOTE_OR_SLASH)
      }
      p == PEEKED_UNQUOTED -> {
        peekedString = nextUnquotedValue()
      }
      p != PEEKED_BUFFERED -> throw JsonDataException("Expected a double but was ${peek()} at path ${getPath()}")
    }

    peeked = PEEKED_BUFFERED

    val result = try {
      peekedString!!.toDouble()
    } catch (e: NumberFormatException) {
      throw JsonDataException("Expected a double but was $peekedString at path ${getPath()}")
    }

    if (!lenient && (result.isNaN() || result.isInfinite())) {
      throw JsonEncodingException("JSON forbids NaN and infinities: $result at path ${getPath()}")
    }

    peekedString = null
    peeked = PEEKED_NONE
    pathIndices[stackSize - 1]++
    return result
  }

  override fun nextLong(): Long {
    val p = peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()
    when {
      p == PEEKED_LONG -> {
        peeked = PEEKED_NONE
        pathIndices[stackSize - 1]++
        return peekedLong
      }
      p == PEEKED_NUMBER -> {
        peekedString = buffer.readUtf8(peekedNumberLength.toLong())
      }
      p == PEEKED_DOUBLE_QUOTED || p == PEEKED_SINGLE_QUOTED -> {
        peekedString = if (p == PEEKED_DOUBLE_QUOTED) nextQuotedValue(DOUBLE_QUOTE_OR_SLASH) else nextQuotedValue(SINGLE_QUOTE_OR_SLASH)
        try {
          val result = peekedString!!.toLong()
          peeked = PEEKED_NONE
          pathIndices[stackSize - 1]++
          return result
        } catch (ignored: NumberFormatException) { // Fall back to parse as a double below.
        }
      }
      p != PEEKED_BUFFERED -> throw JsonDataException("Expected a long but was ${peek()} at path ${getPath()}")
    }

    peeked = PEEKED_BUFFERED

    val asDouble: Double = try {
      peekedString!!.toDouble()
    } catch (e: NumberFormatException) {
      throw JsonDataException("Expected a long but was $peekedString at path ${getPath()}")
    }

    val result = asDouble.toLong()
    if (result.toDouble() != asDouble) { // Make sure no precision was lost casting to 'long'.
      throw JsonDataException("Expected a long but was $peekedString at path ${getPath()}")
    }
    peekedString = null
    peeked = PEEKED_NONE
    pathIndices[stackSize - 1]++
    return result
  }

  override fun nextNumber(): JsonNumber {
    return JsonNumber(nextString()!!)
  }

  /**
   * Returns the string up to but not including `quote`, unescaping any character escape
   * sequences encountered along the way. The opening quote should have already been read. This
   * consumes the closing quote, but does not include it in the returned string.
   *
   * @throws okio.IOException if any unicode escape sequences are malformed.
   */
  private fun nextQuotedValue(runTerminator: ByteString): String {
    var builder: StringBuilder? = null
    while (true) {
      val index = source.indexOfElement(runTerminator)
      if (index == -1L) throw syntaxError("Unterminated string")
      // If we've got an escape character, we're going to need a string builder.
      if (buffer[index] == '\\'.code.toByte()) {
        if (builder == null) builder = StringBuilder()
        builder.append(buffer.readUtf8(index))
        buffer.readByte() // '\'
        builder.append(readEscapeCharacter())
        continue
      }
      // If it isn't the escape character, it's the quote. Return the string.
      return if (builder == null) {
        val result = buffer.readUtf8(index)
        buffer.readByte() // Consume the quote character.
        result
      } else {
        builder.append(buffer.readUtf8(index))
        buffer.readByte() // Consume the quote character.
        builder.toString()
      }
    }
  }

  /** Returns an unquoted value as a string.  */
  private fun nextUnquotedValue(): String {
    val i = source.indexOfElement(UNQUOTED_STRING_TERMINALS)
    return if (i != -1L) buffer.readUtf8(i) else buffer.readUtf8()
  }

  private fun skipQuotedValue(runTerminator: ByteString) {
    while (true) {
      val index = source.indexOfElement(runTerminator)
      if (index == -1L) throw syntaxError("Unterminated string")
      if (buffer[index] == '\\'.code.toByte()) {
        buffer.skip(index + 1)
        readEscapeCharacter()
      } else {
        buffer.skip(index + 1)
        return
      }
    }
  }

  private fun skipUnquotedValue() {
    val i = source.indexOfElement(UNQUOTED_STRING_TERMINALS)
    buffer.skip(if (i != -1L) i else buffer.size)
  }

  override fun nextInt(): Int {
    val p = peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()
    when {
      p == PEEKED_LONG -> {
        val result = peekedLong.toInt()
        if (peekedLong != result.toLong()) { // Make sure no precision was lost casting to 'int'.
          throw JsonDataException("Expected an int but was " + peekedLong
              + " at path " + getPath())
        }
        peeked = PEEKED_NONE
        pathIndices[stackSize - 1]++
        return result
      }
      p == PEEKED_NUMBER -> {
        peekedString = buffer.readUtf8(peekedNumberLength.toLong())
      }
      p == PEEKED_DOUBLE_QUOTED || p == PEEKED_SINGLE_QUOTED -> {
        peekedString = if (p == PEEKED_DOUBLE_QUOTED) nextQuotedValue(DOUBLE_QUOTE_OR_SLASH) else nextQuotedValue(SINGLE_QUOTE_OR_SLASH)
        try {
          val result = peekedString!!.toInt()
          peeked = PEEKED_NONE
          pathIndices[stackSize - 1]++
          return result
        } catch (ignored: NumberFormatException) { // Fall back to parse as a double below.
        }
      }
      p != PEEKED_BUFFERED -> {
        throw JsonDataException("Expected an int but was ${peek()} at path ${getPath()}")
      }
    }

    peeked = PEEKED_BUFFERED

    val asDouble: Double = try {
      peekedString!!.toDouble()
    } catch (e: NumberFormatException) {
      throw JsonDataException("Expected an int but was $peekedString at path ${getPath()}")
    }

    val result = asDouble.toInt()
    if (result.toDouble() != asDouble) { // Make sure no precision was lost casting to 'int'.
      throw JsonDataException("Expected an int but was $peekedString at path ${getPath()}")
    }

    peekedString = null
    peeked = PEEKED_NONE
    pathIndices[stackSize - 1]++
    return result
  }

  override fun close() {
    peeked = PEEKED_NONE
    stack[0] = JsonScope.CLOSED
    stackSize = 1
    buffer.clear()
    source.close()
  }

  override fun skipValue() {
    var count = 0
    do {
      when (peeked.takeUnless { it == PEEKED_NONE } ?: doPeek()) {
        PEEKED_BEGIN_ARRAY -> {
          push(JsonScope.EMPTY_ARRAY)
          count++
        }
        PEEKED_BEGIN_OBJECT -> {
          push(JsonScope.EMPTY_OBJECT)
          count++
        }
        PEEKED_END_ARRAY -> {
          stackSize--
          count--
        }
        PEEKED_END_OBJECT -> {
          stackSize--
          count--
        }
        PEEKED_UNQUOTED_NAME, PEEKED_UNQUOTED -> {
          skipUnquotedValue()
        }
        PEEKED_DOUBLE_QUOTED, PEEKED_DOUBLE_QUOTED_NAME -> {
          skipQuotedValue(DOUBLE_QUOTE_OR_SLASH)
        }
        PEEKED_SINGLE_QUOTED, PEEKED_SINGLE_QUOTED_NAME -> {
          skipQuotedValue(SINGLE_QUOTE_OR_SLASH)
        }
        PEEKED_NUMBER -> {
          buffer.skip(peekedNumberLength.toLong())
        }
      }
      peeked = PEEKED_NONE
    } while (count != 0)
    pathIndices[stackSize - 1]++
    pathNames[stackSize - 1] = "null"
  }

  override fun selectName(names: List<String>): Int {
    if (names.isEmpty()) {
      return -1
    }

    while (hasNext()) {
      val name = nextName()
      val expectedIndex = indexStack[indexStackSize - 1]
      if (names[expectedIndex] == name) {
        return expectedIndex.also {
          indexStack[indexStackSize - 1] = expectedIndex + 1
          if (indexStack[indexStackSize - 1] == names.size) {
            indexStack[indexStackSize - 1] = 0
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
              indexStack[indexStackSize - 1] = index + 1
              if (indexStack[indexStackSize - 1] == names.size) {
                indexStack[indexStackSize - 1] = 0
              }
            }
          }
        }

        skipValue()
      }
    }
    return -1
  }

  private fun push(newTop: Int) {
    if (stackSize == stack.size) throw JsonDataException("Nesting too deep at " + getPath())
    stack[stackSize++] = newTop
  }

  /**
   * Returns the next character in the stream that is neither whitespace nor a part of a comment.
   * When this returns, the returned character is always at `buffer[pos-1]`; this means the caller can always push back the returned
   * character by decrementing `pos`.
   */
  private fun nextNonWhitespace(throwOnEof: Boolean): Int {
    /**
     * This code uses ugly local variables 'p' and 'l' representing the 'pos' and 'limit' fields respectively. Using locals rather than
     * fields saves a few field reads for each whitespace character in a pretty-printed document, resulting in a 5% speedup.
     * We need to flush 'p' to its field before any (potentially indirect) call to fillBuffer() and reread both 'p' and 'l' after any
     * (potentially indirect) call to the same method.
     */
    var p = 0
    loop@ while (source.request(p + 1.toLong())) {
      val c = buffer[p++.toLong()].toInt()
      if (c == '\n'.code || c == ' '.code || c == '\r'.code || c == '\t'.code) {
        continue
      }
      buffer.skip(p - 1.toLong())
      if (c == '/'.code) {
        if (!source.request(2)) {
          return c
        }
        checkLenient()
        val peek = buffer[1]
        return when (peek.toInt().toChar()) {
          '*' -> {
            // skip a /* c-style comment */
            buffer.readByte() // '/'
            buffer.readByte() // '*'
            if (!skipTo("*/")) {
              throw syntaxError("Unterminated comment")
            }
            buffer.readByte() // '*'
            buffer.readByte() // '/'
            p = 0
            continue@loop
          }
          '/' -> {
            // skip a // end-of-line comment
            buffer.readByte() // '/'
            buffer.readByte() // '/'
            skipToEndOfLine()
            p = 0
            continue@loop
          }
          else -> c
        }
      } else if (c == '#'.code) { // Skip a # hash end-of-line comment. The JSON RFC doesn't specify this behaviour, but it's
        // required to parse existing documents.
        checkLenient()
        skipToEndOfLine()
        p = 0
      } else {
        return c
      }
    }
    return if (throwOnEof) {
      throw EOFException("End of input")
    } else {
      -1
    }
  }

  private fun checkLenient() {
    if (!lenient) throw syntaxError("Use JsonReader.setLenient(true) to accept malformed JSON")
  }

  /**
   * Advances the position until after the next newline character. If the line
   * is terminated by "\r\n", the '\n' must be consumed as whitespace by the
   * caller.
   */
  private fun skipToEndOfLine() {
    val index = source.indexOfElement(LINEFEED_OR_CARRIAGE_RETURN)
    buffer.skip(if (index != -1L) index + 1 else buffer.size)
  }

  /**
   * @param toFind a string to search for. Must not contain a newline.
   */
  private fun skipTo(toFind: String): Boolean {
    outer@ while (source.request(toFind.length.toLong())) {
      for (c in toFind.indices) {
        if (buffer[c.toLong()] != toFind[c].code.toByte()) {
          buffer.readByte()
          continue@outer
        }
      }
      return true
    }
    return false
  }

  private fun getPath(): String = JsonScope.getPath(stackSize, stack, pathNames, pathIndices)

  /**
   * Unescapes the character identified by the character or characters that immediately follow a backslash. The backslash '\' should have
   * already been read. This supports both unicode escapes "u000A" and two-character escapes "\n".
   *
   * @throws okio.IOException if any unicode escape sequences are malformed.
   */
  private fun readEscapeCharacter(): Char {
    if (!source.request(1)) throw syntaxError("Unterminated escape sequence")

    return when (val escaped = buffer.readByte().toInt().toChar()) {
      'u' -> {
        if (!source.request(4)) {
          throw EOFException("Unterminated escape sequence at path " + getPath())
        }
        // Equivalent to Integer.parseInt(stringPool.get(buffer, pos, 4), 16);
        var result = 0.toChar()
        var i = 0
        val end = i + 4
        while (i < end) {
          val c = buffer[i.toLong()]
          result = (result.code shl 4).toChar()
          result += when {
            c >= '0'.code.toByte() && c <= '9'.code.toByte() -> (c - '0'.code.toByte())
            c >= 'a'.code.toByte() && c <= 'f'.code.toByte() -> (c - 'a'.code.toByte() + 10)
            c >= 'A'.code.toByte() && c <= 'F'.code.toByte() -> (c - 'A'.code.toByte() + 10)
            else -> throw syntaxError("\\u" + buffer.readUtf8(4))
          }
          i++
        }
        buffer.skip(4)
        result
      }
      't' -> '\t'
      'b' -> '\b'
      'n' -> '\n'
      'r' -> '\r'
      'f' -> '\u000C'
      '\n', '\'', '"', '\\', '/' -> escaped
      else -> {
        if (!lenient) throw syntaxError("Invalid escape sequence: \\$escaped")
        escaped
      }
    }
  }

  override fun rewind() {
    error("BufferedSourceJsonReader cannot rewind.")
  }

  /**
   * Returns a new exception with the given message and a context snippet with this reader's content.
   */
  private fun syntaxError(message: String): JsonEncodingException =
      JsonEncodingException(message + " at path " + getPath())

  companion object {
    private const val MIN_INCOMPLETE_INTEGER = Long.MIN_VALUE / 10
    private val SINGLE_QUOTE_OR_SLASH = "'\\".encodeUtf8()
    private val DOUBLE_QUOTE_OR_SLASH = "\"\\".encodeUtf8()
    private val UNQUOTED_STRING_TERMINALS = "{}[]:, \n\t\r/\\;#=".encodeUtf8()
    private val LINEFEED_OR_CARRIAGE_RETURN = "\n\r".encodeUtf8()
    private const val PEEKED_NONE = 0
    private const val PEEKED_BEGIN_OBJECT = 1
    private const val PEEKED_END_OBJECT = 2
    private const val PEEKED_BEGIN_ARRAY = 3
    private const val PEEKED_END_ARRAY = 4
    private const val PEEKED_TRUE = 5
    private const val PEEKED_FALSE = 6
    private const val PEEKED_NULL = 7
    private const val PEEKED_SINGLE_QUOTED = 8
    private const val PEEKED_DOUBLE_QUOTED = 9
    private const val PEEKED_UNQUOTED = 10

    /** When this is returned, the string value is stored in peekedString.  */
    private const val PEEKED_BUFFERED = 11
    private const val PEEKED_SINGLE_QUOTED_NAME = 12
    private const val PEEKED_DOUBLE_QUOTED_NAME = 13
    private const val PEEKED_UNQUOTED_NAME = 14

    /** When this is returned, the integer value is stored in peekedLong.  */
    private const val PEEKED_LONG = 15
    private const val PEEKED_NUMBER = 16
    private const val PEEKED_EOF = 17

    /* State machine when parsing numbers */
    private const val NUMBER_CHAR_NONE = 0
    private const val NUMBER_CHAR_SIGN = 1
    private const val NUMBER_CHAR_DIGIT = 2
    private const val NUMBER_CHAR_DECIMAL = 3
    private const val NUMBER_CHAR_FRACTION_DIGIT = 4
    private const val NUMBER_CHAR_EXP_E = 5
    private const val NUMBER_CHAR_EXP_SIGN = 6
    private const val NUMBER_CHAR_EXP_DIGIT = 7

    internal const val MAX_STACK_SIZE = 256
  }
}
