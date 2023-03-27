package com.apollographql.apollo3.api.http.internal

import kotlin.native.concurrent.SharedImmutable

@SharedImmutable
private val RESERVED_CHARS = "!#\$&'\"()*+,/:;=?@[]{}% "

/**
 * A very simple urlEncode.
 * Note that spaces are encoded as `%20` and not `+`. See:
 * - https://stackoverflow.com/a/47188851/15695
 * - https://www.rfc-editor.org/rfc/rfc3986#section-2.1
 * - https://github.com/apollographql/apollo-kotlin/pull/4567
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
