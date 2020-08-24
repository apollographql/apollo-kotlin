package com.apollographql.apollo.gradle.internal

import java.util.regex.Pattern

/*
 * Adapted from Gradle GUtil.java so that we can match the task names
 *
 * Copyright 2010 the original author or authors.
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
object GUtil {
  private val WORD_SEPARATOR: Pattern = Pattern.compile("\\W+")

  fun toCamelCase(string: CharSequence?, lower: Boolean): String? {
    if (string == null) {
      return null
    }
    val builder = StringBuilder()
    val matcher = WORD_SEPARATOR.matcher(string)
    var pos = 0
    var first = true
    while (matcher.find()) {
      var chunk = string.subSequence(pos, matcher.start()).toString()
      pos = matcher.end()
      if (chunk.isEmpty()) {
        continue
      }
      if (lower && first) {
        chunk = chunk.decapitalize()
        first = false
      } else {
        chunk = chunk.capitalize()
      }
      builder.append(chunk)
    }
    var rest = string.subSequence(pos, string.length).toString()
    rest = if (lower && first) {
      rest.decapitalize()
    } else {
      rest.capitalize()
    }
    builder.append(rest)
    return builder.toString()
  }
}
