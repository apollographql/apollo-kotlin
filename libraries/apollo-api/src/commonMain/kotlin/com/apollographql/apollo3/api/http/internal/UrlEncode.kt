package com.apollographql.apollo3.api.http.internal

import kotlin.native.concurrent.SharedImmutable

@SharedImmutable
private val RESERVED_CHARS = "!#\$&'\"()*+,/:;=?@[]{} "

/**
 * A very simple urlEncode
 */
internal fun String.urlEncode(): String = buildString {
  this@urlEncode.forEach { char ->
    when (char) {
      in RESERVED_CHARS -> append(char.percentEncode())
      else -> append(char)
    }
  }
}

private fun Char.percentEncode(): String {
  return "%${code.toString(16)}".uppercase()
}
