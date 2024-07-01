package com.apollographql.apollo.ast.internal

/**
 * From https://github.com/cketti/kotlin-codepoints/blob/39abf06857e4542261bf501514668e506d0460e5/kotlin-codepoints/src/commonImplementation/kotlin/CodePoints.kt#L3
 */
private const val MIN_SUPPLEMENTARY_CODE_POINT = 0x10000

private const val MIN_HIGH_SURROGATE = 0xD800
private const val MIN_LOW_SURROGATE = 0xDC00

private const val HIGH_SURROGATE_ENCODE_OFFSET =
    (MIN_HIGH_SURROGATE - (MIN_SUPPLEMENTARY_CODE_POINT ushr 10))

private const val SURROGATE_DECODE_OFFSET =
    MIN_SUPPLEMENTARY_CODE_POINT - (MIN_HIGH_SURROGATE shl 10) - MIN_LOW_SURROGATE

internal fun isBmpCodePoint(codePoint: Int): Boolean {
  return codePoint ushr 16 == 0
}

internal fun highSurrogate(codePoint: Int): Char {
  return ((codePoint ushr 10) + HIGH_SURROGATE_ENCODE_OFFSET).toChar()
}

internal fun lowSurrogate(codePoint: Int): Char {
  return ((codePoint and 0x3FF) + MIN_LOW_SURROGATE).toChar()
}

internal fun codePoint(highSurrogate: Int, lowSurrogate: Int): Int {
  return (highSurrogate shl 10) + lowSurrogate + SURROGATE_DECODE_OFFSET
}

internal fun StringBuilder.appendCodePointMpp(codePoint: Int) {
  if (isBmpCodePoint(codePoint)) {
    append(codePoint.toChar())
  } else {
    append(highSurrogate(codePoint))
    append(lowSurrogate(codePoint))
  }
}