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
package com.apollographql.apollo.api.json.internal

/** Lexical scoping elements within a JSON reader or writer.  */
internal object JsonScope {
  /** An array with no elements requires no separators or newlines before it is closed.  */
  const val EMPTY_ARRAY = 1
  /** A array with at least one value requires a comma and newline before the next element.  */
  const val NONEMPTY_ARRAY = 2
  /** An object with no name/value pairs requires no separators or newlines before it is closed.  */
  const val EMPTY_OBJECT = 3
  /** An object whose most recent element is a key. The next element must be a value.  */
  const val DANGLING_NAME = 4
  /** An object with at least one name/value pair requires a separator before the next element.  */
  const val NONEMPTY_OBJECT = 5
  /** No object or array has been started.  */
  const val EMPTY_DOCUMENT = 6
  /** A document with at an array or object.  */
  const val NONEMPTY_DOCUMENT = 7
  /** A document that's been closed and cannot be accessed.  */
  const val CLOSED = 8

  /**
   * Renders the path in a JSON document to a string.
   *
   * The [pathNames] and [pathIndices] parameters corresponds directly to stack:
   *  - [pathIndices] where the stack contains an object (EMPTY_OBJECT, DANGLING_NAME or NONEMPTY_OBJECT),
   *  - [pathNames] contains the name at this scope.
   *
   * Where it contains an array (EMPTY_ARRAY, NONEMPTY_ARRAY) [pathIndices] contains the current index
   * in that array. Otherwise the value is undefined, and we take advantage of that by incrementing
   * pathIndices when doing so isn't useful.
   */
  fun getPath(stackSize: Int, stack: IntArray, pathNames: Array<String?>, pathIndices: IntArray): List<Any> {
    val result = mutableListOf<Any>()
    for (i in 0 until stackSize) {
      when (stack[i]) {
        EMPTY_ARRAY, NONEMPTY_ARRAY -> result += pathIndices[i]
        EMPTY_OBJECT, DANGLING_NAME, NONEMPTY_OBJECT -> {
          pathNames[i]?.let { result += it }
        }
        NONEMPTY_DOCUMENT, EMPTY_DOCUMENT, CLOSED -> Unit
      }
    }
    return result
  }
}
