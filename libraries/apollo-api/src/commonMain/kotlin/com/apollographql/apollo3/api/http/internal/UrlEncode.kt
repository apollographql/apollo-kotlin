package com.apollographql.apollo3.api.http.internal

import kotlin.native.concurrent.SharedImmutable

@SharedImmutable
private val RESERVED_CHARS = "!#\$&'\"()*+,/:;=?@[]{}"

/**
 * A very simple urlEncode
 */
internal fun String.urlEncode(
    spaceToPlus: Boolean = false
): String = buildString {
  this@urlEncode.forEach {
    when {
      it in RESERVED_CHARS -> append(it.percentEncode())
      spaceToPlus && it == ' ' -> append('+')
      else -> append(it)
    }
  }
}

private fun Char.percentEncode(): String {
  return "%${code.toString(16)}".uppercase()
}
